package cn.com.edtechhub.workmassivelikes.service.impl;

import cn.com.edtechhub.workmassivelikes.contant.ThumbConstant;
import cn.com.edtechhub.workmassivelikes.enums.CodeBindMessage;
import cn.com.edtechhub.workmassivelikes.exception.BusinessException;
import cn.com.edtechhub.workmassivelikes.mapper.ThumbMapper;
import cn.com.edtechhub.workmassivelikes.model.entity.Blog;
import cn.com.edtechhub.workmassivelikes.model.entity.Thumb;
import cn.com.edtechhub.workmassivelikes.service.BlogService;
import cn.com.edtechhub.workmassivelikes.service.ThumbService;
import cn.com.edtechhub.workmassivelikes.service.UserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/*
点赞逻辑是整个项目的优化重点:
1. 使用 MySQL 但是多一个点赞博文关联表,
   先读取点赞表后在把博文列表返回给用户的博文列表中,
   避免重复访问博文点赞情况
2. 使用 MySQL 是基于磁盘读取的还是太慢,
   并且业务上改动不易, 迁移到别的项目比较麻烦,
   使用 Redis 进行优化, Redis 的读写能力至少可以扛到 10w 级别,
   而优化的方向主要是判断用户是否点赞, 主要发生在以下几种情况:
   (1)确认点赞前
   (2)取消点赞前
   (3)批量获取博文前
   (4)根据 id 获取博文前

   这里选择使用 Redis 的 hash 类型, key(user_id): "field(blog_id)=value(thumb_id)"
   这里不要选 blog_id 作为 key, 因为这样在批量获取博文如果需要获取当前用户对这些文章的点赞情况, 就需要进行多次查询
   thumb:001 -> 001=001, 002=002
   thumb:002 -> 001=003, 002=004, 003=5
   thumb:003 -> 001=006
   上面 hash 是根据 user_id 作为 key 来编写的,
   通过 hexists(检查是否存在指定字段) 可以判断用户是否给某篇文章点过赞
   通过 hmget(批量获取多个指定 field 的对应值) 可以获取用户对应文章的点赞数据, 注意 hvals 获取的是所有 field 的对应值
   通过 hset(给 hash key 添加新的 field) 添加新点赞
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
     * 注入事务
     */
    @Resource
    TransactionTemplate transactionTemplate;

    /**
     * 注入 Redis 客户端
     */
    @Resource
    RedisTemplate<String, Object> redisTemplate;

    @Override
    public Boolean thumbAddDo(Long blogId) {
        String userId = userService.userStatus().getUserId();
        // 点赞的时候需要对同一个用户加锁, 否则用户多地登录同时点赞会导致数据不一致性
        synchronized (userId.intern()) { // intern() 把这个字符串变成 JVM 字符串常量池里的唯一对象, 避免每个线程持有的 String 对象不同
            // TODO: 这里的锁是本地锁, 在多实例的分布式场景下失效, 可以使用分布式锁替代(Redisson)
            // 编程式事务(在事务内加锁可能导致锁失效, 线程 A 获取锁 -> 执行数据库操作 -> 释放锁 -> 事务提交, 但是如果线程 B 在 A 释放锁后立即获取锁, 在默认的隔离级别可重复读下, 由于在 A 提交前 B 已经开启了事务, 所以 B 此时只能读到 A 操作前的数据, 导致重复操作, 因此必须让锁的作用域完全包裹事务操作)
            return transactionTemplate.execute( // TODO: 可以在这里执行事务方法, 避免手动写事务
                    status -> {
                        // 检查是否点赞
//                        boolean exists = this.lambdaQuery()
//                                .eq(Thumb::getUserId, Long.valueOf(userId))
//                                .eq(Thumb::getBlogId, blogId)
//                                .exists();

                        // 改为使用 Redis 来进行判断, 不过这样子就需要把数据写入到 Redis 中多维护一份数据
                        Boolean exists = this.hasThumb(blogId, Long.valueOf(userId));

                        if (exists == true) {
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
                        Boolean result = update && this.save(thumb); // 更新成功才执行
                        if (result) { // 由于需要使用 Redis 来判断是否点赞, 因此就需要维护多一份数据, 需要把点赞记录存入 Redis
                            redisTemplate.opsForHash().put(ThumbConstant.USER_THUMB_KEY_PREFIX + userId, blogId.toString(), thumb.getId());
                        }

                        return result; // 从此以后判断是否逻辑都不需要经过数据库了, 不过还需要修改取消点赞的逻辑
                    });
        }
    }

    @Override
    public Boolean thumbAddUnDo(Long blogId) {
        String userId = userService.userStatus().getUserId();
        // 点赞的时候需要对同一个用户加锁, 否则用户多地登录同时点赞会导致数据不一致性
        synchronized (userId.intern()) { // intern() 把这个字符串变成 JVM 字符串常量池里的唯一对象, 避免每个线程持有的 String 对象不同

            // TODO: 这里的锁是本地锁, 在多实例的分布式场景下失效, 可以使用分布式锁替代(Redisson)

            // 编程式事务(在事务内加锁可能导致锁失效, 线程 A 获取锁 -> 执行数据库操作 -> 释放锁 -> 事务提交, 但是如果线程 B 在 A 释放锁后立即获取锁, 在默认的隔离级别可重复读下, 由于在 A 提交前 B 已经开启了事务, 所以 B 此时只能读到 A 操作前的数据, 导致重复操作, 因此必须让锁的作用域完全包裹事务操作)
            return transactionTemplate.execute( // TODO: 可以在这里执行事务方法, 避免手动写事务
                    status -> {
                        // 检查是否点赞
//                        Thumb thumb = this.lambdaQuery()
//                                .eq(Thumb::getUserId, Long.valueOf(userId))
//                                .eq(Thumb::getBlogId, blogId)
//                                .one();

                        // 改为使用 Redis 来进行判断
                        Object thumbIdObj = redisTemplate.opsForHash().get(ThumbConstant.USER_THUMB_KEY_PREFIX + userId, blogId.toString());
                        if (thumbIdObj == null) {
                            throw new BusinessException(CodeBindMessage.CONFLICT_ERROR, "用户已取消点赞");
                        }
                        Long thumbId = Long.valueOf(thumbIdObj.toString());

                        // 更新博客表中对应文章的点赞次数
                        boolean update = blogService.lambdaUpdate()
                                .eq(Blog::getId, blogId)
                                .setSql("thumb_count = thumb_count - 1")
                                .update();

                        // 删除一个点赞记录
                        Boolean result = update && this.removeById(thumbId); // 更新成功才执行

                        // 点赞记录从 Redis 删除
                        if (result) {
                            redisTemplate.opsForHash().delete(ThumbConstant.USER_THUMB_KEY_PREFIX + userId, blogId.toString());
                        }
                        return result;
                    });
        }
    }

    /**
     * 判断某文章是否被某用户点赞过
     */
    private Boolean hasThumb(Long blogId, Long userId) {
        return redisTemplate.opsForHash().hasKey(ThumbConstant.USER_THUMB_KEY_PREFIX + userId, blogId.toString()); // hasKey 其实是对 hExists 命令的封装
    }
}




