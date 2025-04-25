package cn.com.edtechhub.workmassivelikes.service.impl;

import cn.com.edtechhub.workmassivelikes.contant.LuaScriptConstant;
import cn.com.edtechhub.workmassivelikes.contant.ThumbConstant;
import cn.com.edtechhub.workmassivelikes.enums.CodeBindMessage;
import cn.com.edtechhub.workmassivelikes.enums.LuaStatusEnum;
import cn.com.edtechhub.workmassivelikes.exception.BusinessException;
import cn.com.edtechhub.workmassivelikes.mapper.ThumbMapper;
import cn.com.edtechhub.workmassivelikes.model.entity.Blog;
import cn.com.edtechhub.workmassivelikes.model.entity.Thumb;
import cn.com.edtechhub.workmassivelikes.service.BlogService;
import cn.com.edtechhub.workmassivelikes.service.ThumbService;
import cn.com.edtechhub.workmassivelikes.service.UserService;
import cn.com.edtechhub.workmassivelikes.utils.RedisKeyUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Arrays;

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
   thumb:02 -> "001=0003, "002=0004, "003=0005"
   thumb:03 -> "001=0006"
   上面 hash 是根据 user_id 作为 key 来编写的,
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
                                                   thumb:01 -> "001:1" (这里的 1 原本应该填写点赞记录的 thumb_id, 但是改为 1 了, 表示确定点赞, 我们点赞同步持久化可以依赖 MySQL, 直接用 Redis 自己的持久化机制就可以了, 因此点赞记录表不再需要我们维护, 可以考虑直接在 MySQL 中移除, 不过您也可以先保留)
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
   (5)10 s 后创建了新的切面, 用户 01 对 003 确认点赞, 在 Redis 中就有
                                                   thumb:temp:11:20:00 -> "01:001=0", "01:002=1", "01:003=1"
                                                   thumb:temp:11:20:10 -> "01:003=-1" (看到没这种情况就出现了 -1)
                                                   thumb:01 -> "002:1"

   (6)同步机制会在某个时间点启动, 不一定按照切片出现的时间点出现, 但是可以保证每隔一段时间清理临时键值对, 一旦启动就会把临时键值对批量处理以同步到博文点赞总数中, 因此在 Redis 中就有
                                                   thumb:01 -> "002:1", "003:1" (all temp key clean)
   (5)每 10 s 都是重复类似上述的步骤, 这样就可以进行不断的异步备份, 写优化解决了剩下的事情就和之前读优化是一样操作的

   不过在这种情况下, 我们也有很多需要警惕的地方, 确认点赞/取消点赞过程中需要保证原子性, 我们之前对同一个用户进行加锁, 但是我们可以修改为直接使用 Lua 脚本来解决
   另外在前端看来, 如果点赞了立刻刷新, 其实是有点"欺骗"用户的, 因为实际上博文的点赞总数至少在 10 s 内是不会更新的, 因此在用户点赞后需要在前端"虚假"对当前总数 +1, 但是 10 s 后又需要取消 +1, 这还挺麻烦的, 到时候再说...
   在点赞发生意外时, 处理这种异常情况...

   还有一个可以补充的事情, 我们真的要删除数据库中的 thumb 表么? 没必要, 我们基于数据库的点赞服务完全可以作为回退的版本来使用, 我们把原本的点赞逻辑回推到纯粹使用 MySQL 的版本, 以避免 Redis 挂掉无法正常使用

4. [优化过期时间]
   但是这么做还是有些隐患, 我们没有设置过期时间, 并且 Redis 不支持对 hash 字段进行内部字段的过期时间
   只需要在 hash 的 blog_id 字段值修改为 "{"thumbId": "xxx", "expireTime": xxxxxxxxx}" 的 json 即可, 然后使用异步判断缓存是否过期, 大致流程如下:
   (1)用户初次点赞, 会在 Redis 中添加记录, 同时设置时间戳
   (2)用户查询点赞, 会在 Redis 查询, 提高效率, 同时更新时间戳
   (3)用户取消点赞, 会在 Redis 中删除记录
   (4)用户再次点赞, 会在 Redis 中添加记录, 同时设置时间戳
   (5)用户久未登录, 异步虚拟线程工作时, 用 MySQL 备份备份键值对, 用 Redis 删除过期键值对
   (6)用户突然登陆, 由于 Redis 中没有对应的数据, 所以需要查询数据库, 但是还需要把数据库中关于该用户的点赞记录重新推送到 Redis 中, 这样后续读取就不会一直访问两个数据库
*/

/**
 * @author Limou
 * @description 针对表【thumb(点赞表)】的数据库操作Service实现
 * @createDate 2025-04-23 12:30:45
 */
@Service
public class ThumbServiceImpl extends ServiceImpl<ThumbMapper, Thumb> implements ThumbService {

    @Resource
    UserService userService;

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
                            throw new BusinessException(CodeBindMessage.CONFLICT_ERROR, "用户已确认点赞");
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
                            throw new BusinessException(CodeBindMessage.CONFLICT_ERROR, "用户已取消点赞");
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
        String tempThumbKey = RedisKeyUtil.getTempThumbKey(getTimeSlice());
        String userThumbKey = RedisKeyUtil.getUserThumbKey(userId);

        // 执行 Lua 脚本
        long result = redisTemplate.execute(
                LuaScriptConstant.THUMB_SCRIPT,
                Arrays.asList(tempThumbKey, userThumbKey),
                Long.valueOf(userId), // 转回 Long 否则会把 "" 本身也计算进去
                blogId
        );

        if (LuaStatusEnum.FAIL.getValue() == result) {
            throw new BusinessException(CodeBindMessage.CONFLICT_ERROR, "用户已确认点赞");
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
            throw new BusinessException(CodeBindMessage.CONFLICT_ERROR, "用户已取消点赞");
        }

        return LuaStatusEnum.SUCCESS.getValue() == result;
    }

    /**
     * 判断某文章是否被某用户点赞过
     */
    private Boolean hasThumb(Long blogId, Long userId) {
        return redisTemplate
                .opsForHash() // 获得操作 hash 的对象
                .hasKey(ThumbConstant.USER_THUMB_KEY_PREFIX + userId, blogId.toString()); // hasKey 其实是对 hexists 命令的封装
    }

    /**
     * 获取时间切面字符串
     */
    private String getTimeSlice() {
        DateTime nowDate = DateUtil.date();
        // 获取到当前时间前最近的整数, 比如当前 01:30:13, 获取到 01:30:10
        return DateUtil.format(nowDate, "HH:mm:") + (DateUtil.second(nowDate) / 10) * 10;
    }

}




