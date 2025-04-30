package cn.com.edtechhub.workmassivelikes.service.impl;

import cn.com.edtechhub.workmassivelikes.cache.AddResult;
import cn.com.edtechhub.workmassivelikes.cache.HeavyKeeper;
import cn.com.edtechhub.workmassivelikes.contant.LuaScriptConstant;
import cn.com.edtechhub.workmassivelikes.enums.CodeBindMessageEnum;
import cn.com.edtechhub.workmassivelikes.enums.LuaStatusEnum;
import cn.com.edtechhub.workmassivelikes.exception.BusinessException;
import cn.com.edtechhub.workmassivelikes.mapper.ThumbMapper;
import cn.com.edtechhub.workmassivelikes.model.dto.ThumbEventDto;
import cn.com.edtechhub.workmassivelikes.model.entity.Blog;
import cn.com.edtechhub.workmassivelikes.model.entity.Thumb;
import cn.com.edtechhub.workmassivelikes.service.BlogService;
import cn.com.edtechhub.workmassivelikes.service.ThumbService;
import cn.com.edtechhub.workmassivelikes.service.UserService;
import cn.com.edtechhub.workmassivelikes.utils.RedisKeyUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.pulsar.core.PulsarTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/*
点赞逻辑是整个项目的优化重点:

1. [避免重复查]
   使用 MySQL 但是多一个点赞博文关联表, 先读取点赞表后在把博文列表返回给用户的博文列表中, 避免重复访问博文点赞情况

2. [优化读能力]
   使用 MySQL 是基于磁盘读取的还是太慢, 使用 Redis 进行优化, Redis 的读写能力至少可以扛到 10w 级别,
   而优化的方向主要是判断用户是否点赞, 主要发生在以下几种情况:
   (1)确认点赞前
   (2)取消点赞前
   (3)批量获取博文前
   (4)根据 id 获取博文前

   这里选择使用 Redis 的 hash 类型, key(user_id): "field(blog_id)=value(thumb_id)"
   这里不要选 blog_id 作为 key, 因为这样在批量获取博文如果需要获取当前用户对这些文章的点赞情况, 就需要进行多次查询
   thumb:01 -> "001=0001", "002=0002"
   thumb:02 -> "001=0003", "002=0004", "003=0005"
   thumb:03 -> "001=0006"
   上面 hash 是根据 user_id 作为 key 来编写的

   通过 hexists(检查是否存在指定字段) 可以判断用户是否给某篇文章点过赞
   通过 hmget(批量获取多个指定 field 的对应值) 可以获取用户对应文章的点赞数据, 注意 hvals 获取的是所有 field 的对应值
   通过 hset(给 hash key 添加新的 field) 添加新点赞

3. [优化写能力]
   每次 "确认点赞/取消点赞" 都直接操作数据库, 当很多用户同时点赞一篇博客时, 对应的博客记录就会成为热点行, 因为这样会每一次点赞都会同步修改博文的点赞次数
   很明显在写入能力上可以优化:
   (1)频繁写入数据库增加数据库负载
   (2)高并发场景下可能出现性能瓶颈
   (3)事务操作导致响应时间变长

   因此可以把确认点赞和取消点赞的逻辑先存储到 Redis 中, 不直接同步到 MySQL 中, 后续定时同步进数据库即可
   依旧是 Redis 的 hash 类型, key(user_id): "field(blog_id)=value(thumb_id)", 这种情况下我们的键值对结构就不够用了
   我们需要新增加临时的 key(time_slice): "field(user_id:blog_id)=value(is_thumb)", 其中 is_thumb 值为 确认点赞(1)、没有变化(0)、取消点赞(-1), 之所以出现没有变化的情况是因为有可能在时间切片期间内用户确认点赞后就立马取消点赞了, 相当无效操作

   简单说一下流程(每 10 s 做一次切片):
   (1)10 s 内用户 01 对 001 确认点赞, 在 Redis 中就有
                                                   thumb:temp:11:20:00 -> "01:001=1"
                                                   thumb:01 -> "001:1" (这里的 1 原本应该填写点赞记录的 thumb_id, 但是改为 1 了, 表示确定点赞, 后续持久化到 MySQL 中后会自动分配 id, 没必要在从数据库中获取)
   (2)10 s 内用户 01 对 001 取消点赞, 在 Redis 中就有
                                                   thumb:temp:11:20:00 -> "01:001=0"
   (3)10 s 内用户 01 对 002 确认点赞, 在 Redis 中就有
                                                   thumb:temp:11:20:00 -> "01:001=0", "01:002=1"
                                                   thumb:01 -> "002:1"
   (4)10 s 内用户 01 对 002 重复点赞, 在 Redis 中就有
                                                   thumb:temp:11:20:00 -> "01:001=0", "01:002=1"
                                                   thumb:01 -> "002:1"
   (5)10 s 内用户 01 对 003 确认点赞, 在 Redis 中就有
                                                   thumb:temp:11:20:00 -> "01:001=0", "01:002=1", "01:003=1"
                                                   thumb:01 -> "002:1", "003:1"
   (5)10 s 后创建了新的切面, 用户 01 对 003 取消点赞, 在 Redis 中就有
                                                   thumb:temp:11:20:00 -> "01:001=0", "01:002=1", "01:003=1"
                                                   thumb:temp:11:20:10 -> "01:003=-1" (看到没这种情况就出现了 -1, 这种情况出现在同步机制还没有启动时)
                                                   thumb:01 -> "002:1"

   (6)同步机制会在某个时间点启动, 但是不一定按照切片出现的时间点出现, 但是可以保证每隔一段时间清理临时键值对, 一旦启动就会把临时键值对批量处理以同步到博文点赞总数中, 因此在 Redis 中就有
                                                   thumb:01 -> "002:1", "003:1" (all temp key clean)
   (7)每 10 s 都是重复类似上述的步骤, 这样就可以进行不断的异步备份, 写优化解决了剩下的事情就和之前读优化是一样操作的

   (8)不过在这种情况下, 我们也有很多需要警惕的地方, 确认点赞/取消点赞过程中需要保证原子性, 我们之前对同一个用户进行加锁, 但是我们可以修改为直接使用 Lua 脚本来解决
   (9)另外在前端看来, 如果点赞了立刻刷新, 其实是有点"欺骗"用户的, 因为实际上博文的点赞总数至少在 10 s 内是不会更新的, 因此在用户点赞后需要在前端"虚假"对当前总数 +1, 但是 10 s 后又需要取消 +1, 这还挺麻烦的, 到时候再说...
   (10)在点赞发生意外时, 处理这种异常情况...

4. [增加补偿机制]
   一些极限情况下, 系统异常将导致数据不一致问题, 因为我们上面的设计是根据时间片来备份到 MySQL 中的, 可能会出现备份时部分旧的分片没有被备份, 我们还需要实现一个补偿任务, 在 job 包下创建 SyncThumb2DBCompensatoryJob

5. [多级缓存优化]
   在访问速度问题上, 我们还可以使用多级缓存问题来进一步提高访问速度
   (1)一级缓存 Caffeine, 这里保存访问最高频的数据, 响应速度最快(Caffeine 将数据直接存储在 JVM 堆内存, 并且由于是基于本地的缓存机制, 很少网络开销)
   (2)二级缓存 Redis, 这里保存访问次数较多的数据, 响应速度较快(Redis 是基于内存的缓存机制, 但是由于 Redis 是基于网络的, 因此网络开销比较大)
   (3)三级缓存 MySQL, 最终存储层, 保证数据的持久化和完整性
   引入 Caffeine 比较简单, 但是我们需要明白我们拿 Caffeine 来缓存什么数据, 这里把非常热点的键值对使用 Caffeine 进行本地存储
   我把这里引入的 Caffeine 留到后面的超级热点中进行使用

6. [超级热点优化]
   我们通过引入 Redis 解决了点赞系统的数据库读写压力问题, 虽然 Redis 性能优于 MySQL, 但在高并发场景下, Redis 也可能成为系统瓶颈:
   - 单点热点问题: 某些热门内容(如爆款博客)会被大量用户同时点赞, Redis 服务器压力激增
   - 恶意用户刷量: 某些恶意用户用脚本超高频率刷点赞接口, 导致对应的 hash 中的某个 key 成为超级热点, 影响正常用户的请求响应
   这些问题在流量高峰期尤为明显, 可能导致系统响应变慢, 甚至服务不可用, 我们需要进一步优化 "用户是否已点赞" 的判断逻辑, 减轻 Redis 的压力

   这里选择自己实现 TopK 算法, HeavyKeeper 是一种高效的流式 TopK 检测算法, 专为识别大规模数据流中的频繁项(热点 Key)而生, 它基于 Count-Min Sketch 算法改进, 主要通过以下组件实现:
   (1)二维数组: 算法维护一个 d*w 二维数组, 里面有 d 层, 每层里有 w 个桶, 桶里记录哈希指纹和计数值
   (2)小堆结构: 维护一个大小为 k 的最小堆, 用于记录当前观测到的 TopK 项
   (3)队列结构: 维护一个队列, 用于存储被挤出最小堆的元素
   (4)映射机制: 通过哈希函数将数据映射到二维数组中, 每个数据都会映射到一个桶中, 桶中记录哈希指纹和计数值
   (5)衰减机制: 核心创新点, 当发生哈希冲突时, 不是简单的覆盖, 而是通过概率衰减原有计数

   整理一下, 我们需要优化的主要是 "用户是否已点赞" 的判断逻辑, 因此在下面这些地方都需要进行优化
   - 用户确认点赞之前
   - 用户取消点赞之前

   需要使用 TopK 算法得到的热点数据 userId 列表, 然后 Caffeine 中就需要存储根据 userId 以及对应的点赞记录列表, 这样在用户 blogId 点赞操作之前, 就可以直接从 Caffeine 中获取是否已经点赞, 而不需要再去 Redis 中判断
   例如对于 Redis 中的 thumb:01 -> "002:1", "003:1", 我们可以在 Caffeine 中存储 "01:[002, 003]",
   这样在用户确认点赞 002 之前, 就可以直接从 Caffeine 中获取是否已经确认点赞, 而不需要再去 Redis 中判断
   这样在用户取消点赞 003 之前, 就可以直接从 Caffeine 中获取是否已经取消点赞, 而不需要再去 Redis 中判断(可选)

   其他的情况下如果需要实现 L1 缓存再说, 先把这个最重要的实现了...

7. [消息队列异步]
   不过在系统中还是存在一些问题:
   - 点赞操作与后续的数据处理强耦合(如博文点赞计数更新), 不利于其他服务功能扩展
   - Redis 和数据库之间的数据同步依赖定时任务, 缺乏实时性和可靠性保障, MySQL 挂掉后无所作为
   因此我们需要引入消息队列, 这里选择使用 Pulsar 作为消息队列

   (1)用户发起点赞请求后, 事件类型为 INCR, 服务端首先在 Redis/Caffeine 中验证用户是否已点赞
   (2)对于未点赞的用户, 立即更新 Redis 中的点赞状态, 但是这次我们不再自己制作临时键值对, 我们为什么引入这个临时的机制? 不就是为了在某个时间片下进行备份么, 引入 MQ 可以快速帮助我们解决这个问题
   (3)生产者构造点赞事件异步发送到 Pulsar 消息队列, 然后立即返回成功响应给用户
   (4)消费者则异步处理队列中的点赞事件, 将数据持久化到数据库, 并更新博客的点赞计数

   取消点赞的流程类似, 区别在于事件类型变为 DECR, 实际上引入了 MQ 后让系统变得更加简单了

   不过有些时候会有一些异常网络波动, 会导致消息消费失败, 这个时候就需要做重试操作, 只需要配置网络协议栈中的重发机制 ACK 和 NACK 即可, 这个很简单, 只需要配置一个 Bean 后配置到 @PulsarListener 即可
   如果出现无论如何重试都无法解决的消息, 则需要将消息存入到死信队列, 然后通过通知相关人员的方式来处理异常情况, 确保消息能够被成功消费掉, 也比较简单, 只需要配置一个 Bean 后配置到 @PulsarListener 即可
   不过不知道为什么无法使用上述三个设置, 这点可能需要仔细查阅文档(会导致 consumerCustomizer 失效)

   另外还需要设置和之前机制类似的补偿定时任务, 不过需要注意一种极端情况,
   如果用户在 2 点时点赞, 消息队列还没来得及消费该消息时刚好对账任务开始执行,
   对账发现缓存与数据库不一致产生一条补偿消息, 与正常消息重复, 由于唯一键的存在, 多次插入失败后消息就会进入死信队列
   处理方式也很简单, 把点赞记录的 value 改成点赞时的时间戳或具体时间, 在对账任务中只比对当天 1 点之前的数据(给足消息队列处理消息的时间)即可
   有时间可以处理一下

   另外还可以继续结合本地缓存的方案, 有时间加上
   这个问题可以后面再说...

8. [修复非法点赞]
   由于我们跳过了一层数据库的校验, 把点赞的逻辑直接迁移到了 Redis 中, 然后开启定期的备份, 但此时无法校验接口中的 blogId 是否存在, 这会导致用户理论上来说可以对不存在的文章进行点赞
   查一次数据库校验在目前阶段肯定是不太合理了, 读一次库的时间都会超过我们写 Redis 的时间, 影响到系统的并发
   可以通过额外存储在 Redis 中的 String 结构来判断博客是否存在, 并且在 Lua 脚本里加一次判断即可
   这个问题可以后面再说...

9. [优化过期时间]
   但是这么做还是有些隐患, 我们没有设置过期时间, 并且 Redis 不支持对 hash 字段进行内部字段的过期时间
   只需要在 hash 的 blog_id 字段值修改为 "{"thumbId": "xxx", "expireTime": xxxxxxxxx}" 的 json 即可, 然后使用异步判断缓存是否过期, 大致流程如下:
   (1)用户初次点赞, 会在 Redis 中添加记录, 同时设置时间戳
   (2)用户查询点赞, 会在 Redis 查询, 提高效率, 同时更新时间戳
   (3)用户取消点赞, 会在 Redis 中删除记录
   (4)用户再次点赞, 会在 Redis 中添加记录, 同时设置时间戳
   (5)用户久未登录, 异步虚拟线程工作时, 用 MySQL 备份备份键值对, 用 Redis 删除过期键值对
   (6)用户突然登陆, 由于 Redis 中没有对应的数据, 所以需要查询数据库, 但是还需要把数据库中关于该用户的点赞记录重新推送到 Redis 中, 这样后续读取就不会一直访问两个数据库
   这个问题可以后面再说...

10. [分布式数据库]
   单机的性能是有极限的, 我们需要分布式的数据库, 但是传统的 MySQL 数据库集群方案比较复杂, 因此我们选择使用 TiDB 来替代 MySQL
   最基础的 TiDB 测试集群通常由 2 个 TiDB 实例、3 个 TiKV 实例、3 个 PD 实例、可选的 TiFlash 实例构成
   - TiDB Server: 负责 SQL 解析和请求处理
   - TiKV Server: 数据分片存储
   - PD Server: 确保调度中心高可用
   - TiFlash: 用于分析查询加速
   通过 TiUP Playground 可以快速搭建出上述的一套基础测试集群, 另外个人提议, 不要使用 Docker 部署和数据状态有关的组件, 尤其是需要持久化的数据, 并且也推荐直接部署在存储用的机器上

   由于 TiDB 兼容 MySQL 协议, 所以很多操作是和 MySQL 类似的

   curl --proto '=https' --tlsv1.2 -sSf https://tiup-mirrors.pingcap.com/install.sh | sh && source ~/.bashrc
   tiup playground --tag thumb / tiup playground --tag thumb --tiflash 0 / tiup playground --tag thumb --without-monitor --tiflash 0  (控制台账户默认为 root 密码默认为空)
   tiup playground --tag thumb cleanup
   - 全量数据同步: 使用 TiDB DM 工具进行初始数据迁移
   - 增量数据同步: 配置 MySQL Binlog 到 TiDB 的实时同步
   - 双写验证阶段: 应用同时写入 MySQL 和 TiDB，比对数据一致性
   - 切换读流量: 将读请求切换到 TiDB
   - 切换写流量: 确认无问题后将写请求切换到 TiDB
   - 下线旧 MySQL: 完成迁移后, 逐步下线 MySQL 实例
   - IDEA 快速迁移: 导入/导出 -> mysqldump -> ...

   和 MySQL 的 3306 类似, TIDB 的端口号为 4000, 然后修改配置文件中的端口号就够了, 整个过程是很丝滑, 只需要运维人员集群化 TIDB 即可

11. [高强压力测试]
   在 JMeter 中设置 "启动时间、线程数、循环次数" 这 3 个值, 线程数 * 循环次数 = 要测试的请求总数, 启动时间的作用是控制线程的启动速率从而控制请求速率

   线程数：5010 个 / 组
   启动时长：5 秒
   循环次数：10 组

12. [可观测性优化]
   使用 tiup playground 命令一键启动了 TiDB, 同时这个命令会默认启动 Prometheus 和 Grafana, 它跟 tiup 绑定的比较深, 用起来没有那么方便
   为了防止它跟我们后续手动安装的监控组件冲突, 需要在启动 TiDB 时禁止这两个组件, 通过 --help 命令得知, --without-monitor 可以实现这个需求

   为了监控 Redis 的性能和缓存命中率, 我们需要添加 Redis Exporter
   这个问题可以后面再说...

13. [高可用优化]
   比如目前系统仍然存在单点故障风险, 当 MySQL/TiDB、Redis、Pulsar 出现故障时, 可能导致整个服务不可用, 缺乏有效的降级机制和容错策略
   因此需要从几个角度来实现高可用, 不过无论是哪个组件出现不可用, 都是以抛出异常为表现, 就可以引入 Sentinel 组件来进行异常检测, 然后再进行熔断降级处理
   - 数源高可用: 不使用 MySQL, 使用 TiDB 作为分布式数据库已具备基础的高可用性, 但仍可以进一步增强容灾能力, 比如通过
        (1)多中心部署: 在不同地域部署 TiDB 集群, 熔断降级为不同集群的数据中心, 平时这些数据中心是工作数据中心和备份数据中心的关系
        (2)两地三中心架构: 更高级别的保护可采用两地三中心架构, 包括工作数据中心、同城灾备数据中心、异地灾备数据中心, 防范城市级灾难
   - 缓存高可用: 在极端情况下, 当 Redis 服务不可用时, 扩大 Caffeine 本地缓存容量, 临时存储热点数据, 同时对非热点数据降级为直接查询数据库, 同时解决 "缓存击穿、缓存雪崩、缓存穿透" 的问题
        (1)主从复制架构
        (2)哨兵模式架构
        (3)集群节点架构
        还可以引入布隆过滤器来实现针对缓存的防护机制
   - 队列高可用: Pulsar 也天然支持集群化, 将异步点赞处理转为同步处理, 也可以采用我们已经实现过的策略, 使用 Redis 暂存点赞信息, 然后批量同步到数据库
   这个问题可以后面再说...
*/

/**
 * 点赞服务实现
 *
 * @author <a href="https://github.com/limou3434">limou3434</a>
 */
@Service
@Slf4j
public class ThumbServiceImpl extends ServiceImpl<ThumbMapper, Thumb> implements ThumbService {

    /**
     * 注入用户服务依赖
     */
    @Resource
    UserService userService;

    /**
     * 注入博文服务依赖
     */
    @Resource
    BlogService blogService;

    /**
     * 注入事务依赖
     */
    @Resource
    TransactionTemplate transactionTemplate;

    /**
     * 注入 Redis 客户端依赖
     */
    @Resource
    RedisTemplate<String, Object> redisTemplate;

    /**
     * 初始化 TopK 数据结构
     */
    @Resource
    HeavyKeeper hotKeyDetector;

    /**
     * 初始化 Caffeine 本地缓存
     */
    @Resource
    Cache<String, Object> localCache;

    /**
     * 注入 Pulsar 客户端
     */
    @Resource
    private PulsarTemplate<ThumbEventDto> pulsarTemplate;

    @Override
    public Boolean thumbAddDoUseMySQL(Long blogId) {
        String userId = userService.userStatus().getUserId();
        // 点赞的时候需要对同一个用户加锁, 否则用户多地登录同时点赞会导致数据不一致性
        synchronized (userId.intern()) { // intern() 把这个字符串变成 JVM 字符串常量池里的唯一对象, 避免每个线程持有的 String 对象不同
            // TODO: 这里的锁是本地锁, 在多实例的分布式场景下失效, 可以使用分布式锁替代(Redisson)
            // 编程式事务(在事务内加锁可能导致锁失效, 线程 A 获取锁 -> 执行数据库操作 -> 释放锁 -> 事务提交, 但是如果线程 B 在 A 释放锁后立即获取锁, 在默认的隔离级别可重复读下, 由于在 A 提交前 B 已经开启了事务, 所以 B 此时只能读到 A 操作前的数据, 导致重复操作, 因此必须让锁的作用域完全包裹事务操作)
            return transactionTemplate.execute( // TODO: 可以在这里执行事务方法, 避免手动写事务
                    status -> {
                        // 检查是否点赞
                        boolean exists = this.lambdaQuery()
                                .eq(Thumb::getUserId, Long.valueOf(userId))
                                .eq(Thumb::getBlogId, blogId)
                                .exists();

                        if (exists) {
                            // 有可能缓存中没有记录, 还需要进一步查询 MySQL, 但是单纯这么想就会违背我们引入缓存的目的(高效)
                            throw new BusinessException(CodeBindMessageEnum.CONFLICT_ERROR, "用户已确认点赞");
                        }

                        // 更新博客表中对应文章的点赞次数
                        boolean update = blogService.lambdaUpdate()
                                .eq(Blog::getId, blogId)
                                .setSql("thumb_count = thumb_count + 1")
                                .update();

                        // 添加一个点赞记录
                        Thumb thumb = new Thumb();
                        thumb.setUserId(Long.valueOf(userId));
                        thumb.setBlogId(blogId);
                        return update && this.save(thumb); // 更新成功才执行
                    });
        }
    }

    @Override
    public Boolean thumbAddUnDoUseMySQL(Long blogId) {
        String userId = userService.userStatus().getUserId();
        // 点赞的时候需要对同一个用户加锁, 否则用户多地登录同时点赞会导致数据不一致性
        synchronized (userId.intern()) { // intern() 把这个字符串变成 JVM 字符串常量池里的唯一对象, 避免每个线程持有的 String 对象不同

            // TODO: 这里的锁是本地锁, 在多实例的分布式场景下失效, 可以使用分布式锁替代(Redisson)

            // 编程式事务(在事务内加锁可能导致锁失效, 线程 A 获取锁 -> 执行数据库操作 -> 释放锁 -> 事务提交, 但是如果线程 B 在 A 释放锁后立即获取锁, 在默认的隔离级别可重复读下, 由于在 A 提交前 B 已经开启了事务, 所以 B 此时只能读到 A 操作前的数据, 导致重复操作, 因此必须让锁的作用域完全包裹事务操作)
            return transactionTemplate.execute( // TODO: 可以在这里执行事务方法, 避免手动写事务
                    status -> {
                        // 检查是否点赞
                        Thumb thumb = this.lambdaQuery()
                                .eq(Thumb::getUserId, Long.valueOf(userId))
                                .eq(Thumb::getBlogId, blogId)
                                .one();

                        if (thumb == null) {
                            throw new BusinessException(CodeBindMessageEnum.CONFLICT_ERROR, "用户已取消点赞");
                        }

                        Long thumbId = thumb.getId();

                        // 更新博客表中对应文章的点赞次数
                        boolean update = blogService.lambdaUpdate()
                                .eq(Blog::getId, blogId)
                                .setSql("thumb_count = thumb_count - 1")
                                .update();

                        // 删除一个点赞记录
                        return update && this.removeById(thumbId); // 更新成功才执行
                    });
        }
    }

    @Override
    public Boolean thumbAddDoUseRedis(Long blogId) {
        String userId = userService.userStatus().getUserId();
        String tempThumbKey = RedisKeyUtil.getTempThumbKey(getTimeSlice()); // key(time_slice) -> "field(user_id:blog_id)=value(is_thumb)", ...
        String userThumbKey = RedisKeyUtil.getUserThumbKey(userId); // key(user_id) -> "field(blog_id)=value(thumb_id)", ...

        // 执行 Lua 脚本
        long result = redisTemplate.execute(
                LuaScriptConstant.THUMB_SCRIPT,
                Arrays.asList(tempThumbKey, userThumbKey),
                Long.valueOf(userId), // 转回 Long 否则会把 "" 本身也计算进去
                blogId
        );

        if (LuaStatusEnum.FAIL.getValue() == result) {
            throw new BusinessException(CodeBindMessageEnum.CONFLICT_ERROR, "用户已确认点赞");
        }

        return LuaStatusEnum.SUCCESS.getValue() == result;
    }

    @Override
    public Boolean thumbAddUnDoUseRedis(Long blogId) {
        String userId = userService.userStatus().getUserId();
        String tempThumbKey = RedisKeyUtil.getTempThumbKey(getTimeSlice());
        String userThumbKey = RedisKeyUtil.getUserThumbKey(userId);

        // 执行 Lua 脚本
        long result = redisTemplate.execute(
                LuaScriptConstant.UNTHUMB_SCRIPT,
                Arrays.asList(tempThumbKey, userThumbKey),
                Long.valueOf(userId),
                blogId
        );

        if (result == LuaStatusEnum.FAIL.getValue()) {
            throw new BusinessException(CodeBindMessageEnum.CONFLICT_ERROR, "用户已取消点赞");
        }

        return LuaStatusEnum.SUCCESS.getValue() == result;
    }

    @Override
    public Boolean thumbAddDoUseCaffeine(Long blogId) {
        String userId = userService.userStatus().getUserId();
        String userThumbKey = RedisKeyUtil.getUserThumbKey(userId); // key(user_id) -> "field(blog_id)=value(thumb_id)", ...

        // 添加/更新新的元素
        AddResult addResult = hotKeyDetector.add(userId, 1);

        log.debug("当前操作 {}", addResult.getCurrentKey());
        log.debug("被挤出的 {}", addResult.getExpelledKey());
        log.debug("是否热点 {}", addResult.isHotKey());
        log.debug("TopK list {}", hotKeyDetector.list());
        log.debug("TopK expelled {}", hotKeyDetector.expelled());

        // 如果当前元素是热点, 则需要把当前元素的 key 存储到 Caffeine 中
        if (addResult.isHotKey()) {
            // 如果 Caffeine 中已经存在了, 则表示用户已经点赞过了
            if (localCache.getIfPresent(userId) != null) {
                log.debug("用户 {} 反复点赞情况较多, 可以警告或封禁", userId);
                throw new BusinessException(CodeBindMessageEnum.CONFLICT_ERROR, "用户已确认点赞");
            }
            // 如果 Caffeine 中不存在, 则表示用户没有点赞过, 需要把当前元素的 key 存储到 Caffeine 中
            else {
                localCache.put(userId, userThumbKey);
            }
        }

        // 如果当前元素不是热点, 则直接进行点赞操作即可
        return this.thumbAddDoUseRedis(blogId);
    }

    @Override
    public Boolean thumbAddUnDoUseCaffeine(Long blogId) {
        String userId = userService.userStatus().getUserId();

        // 如果用户在 Caffeine 中存在，则移除
        if (localCache.getIfPresent(userId) != null) {
            localCache.invalidate(userId);
            log.debug("用户 {} 取消点赞, 移除 Caffeine 缓存", userId);
        }

        // 进行取消点赞的实际操作
        return this.thumbAddUnDoUseRedis(blogId);
    }

    public Boolean thumbAddDoUseMQ(Long blogId) {
        String userId = userService.userStatus().getUserId();

        log.debug("用户 {} 确认点赞博客 {}", userId, blogId);

        String userThumbKey = RedisKeyUtil.getUserThumbKey(userId);

        // 执行 Lua 脚本
        long result = redisTemplate.execute(
                LuaScriptConstant.THUMB_SCRIPT_MQ,
                List.of(userThumbKey),
                blogId
        );

        if (LuaStatusEnum.FAIL.getValue() == result) {
            throw new BusinessException(CodeBindMessageEnum.CONFLICT_ERROR, "用户已确认点赞");
        }

        // 构造确认点赞事件
        ThumbEventDto thumbEvent = ThumbEventDto.builder()
                .blogId(blogId)
                .userId(Long.valueOf(userId))
                .type(ThumbEventDto.EventType.INCR)
                .eventTime(LocalDateTime.now())
                .build();

        // 异步发送确认点赞事件到 Pulsar 消息队列
        pulsarTemplate
                .sendAsync("thumb-topic", thumbEvent) // 发送到 Pulsar 消息队列中的 thumb-topic 主题中
                .exceptionally(ex -> { // 出现异常时的处理
                    redisTemplate.opsForHash().delete(userThumbKey, blogId.toString(), true);
                    log.debug("确认点赞事件发送失败: userId={}, blogId={}", userId, blogId);
                    return null;
                });

        return true;
    }

    public Boolean thumbAddUnDoUseMQ(Long blogId) {
        String userId = userService.userStatus().getUserId();
        String userThumbKey = RedisKeyUtil.getUserThumbKey(userId);

        log.debug("用户 {} 取消点赞博客 {}", userId, blogId);

        // 执行 Lua 脚本
        long result = redisTemplate.execute(
                LuaScriptConstant.UNTHUMB_SCRIPT_MQ,
                List.of(userThumbKey),
                blogId
        );

        if (LuaStatusEnum.FAIL.getValue() == result) {
            throw new BusinessException(CodeBindMessageEnum.CONFLICT_ERROR, "用户已取消点赞");
        }

        // 构造取消点赞事件
        ThumbEventDto thumbEventDto = ThumbEventDto.builder()
                .blogId(blogId)
                .userId(Long.valueOf(userId))
                .type(ThumbEventDto.EventType.DECR)
                .eventTime(LocalDateTime.now())
                .build();

        // 异步发送取消点赞事件到 Pulsar 消息队列
        pulsarTemplate
                .sendAsync("thumb-topic", thumbEventDto)
                .exceptionally(ex -> {
                    redisTemplate.opsForHash().put(userThumbKey, blogId.toString(), true);
                    log.debug("取消点赞事件发送失败: userId={}, blogId={}", userId, blogId);
                    return null;
                });

        return true;
    }

    /**
     * 获取时间切片字符串
     */
    private String getTimeSlice() {
        DateTime nowDate = DateUtil.date();
        // 获取到当前时间前最近的整数, 比如当前 01:30:13, 获取到 01:30:10
        return DateUtil.format(nowDate, "HH:mm:") + (DateUtil.second(nowDate) / 10) * 10;
    }

}
