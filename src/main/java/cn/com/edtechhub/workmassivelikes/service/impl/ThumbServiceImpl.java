package cn.com.edtechhub.workmassivelikes.service.impl;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

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

    @Resource
    TransactionTemplate transactionTemplate;

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
                        boolean exists = this.lambdaQuery()
                                .eq(Thumb::getUserId, Long.valueOf(userId))
                                .eq(Thumb::getBlogId, blogId)
                                .exists();
                        if (exists) {
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
    public Boolean thumbAddUnDo(Long blogId) {
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

                        // 更新博客表中对应文章的点赞次数
                        boolean update = blogService.lambdaUpdate()
                                .eq(Blog::getId, blogId)
                                .setSql("thumb_count = thumb_count - 1")
                                .update();

                        // 删除一个点赞记录
                        return update && this.removeById(thumb.getId()); // 更新成功才执行
                    });
        }
    }

}




