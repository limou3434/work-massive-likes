package cn.com.edtechhub.workmassivelikes.service;

import cn.com.edtechhub.workmassivelikes.model.entity.Thumb;
import cn.com.edtechhub.workmassivelikes.request.ThumbDoRequest;
import cn.com.edtechhub.workmassivelikes.request.ThumbUnDoRequest;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 点赞服务声明
 *
 * @author <a href="https://github.com/limou3434">limou3434</a>
 */
public interface ThumbService extends IService<Thumb> {

    /**
     * 确认点赞(基于 MySQL)
     */
    Boolean thumbAddDoUseMySQL(Long blogId);

    /**
     * 取消点赞(基于 MySQL)
     */
    Boolean thumbAddUnDoUseMySQL(Long blogId);

    /**
     * 确认点赞(基于 Redis)
     */
    Boolean thumbAddDoUseRedis(Long blogId);

    /**
     * 取消点赞(基于 Redis)
     */
    Boolean thumbAddUnDoUseRedis(Long blogId);

    /**
     * 确认点赞(基于 Caffeine)
     */
    Boolean thumbAddDoUseCaffeine(Long blogId);

    /**
     * 取消点赞(基于 Caffeine)
     */
    Boolean thumbAddUnDoUseCaffeine(Long blogId);

    /**
     * 确认点赞(基于 MQ)
     */
    Boolean thumbAddDoUseMQ(Long blogId);

    /**
     * 取消点赞(基于 MQ)
     */
    Boolean thumbAddUnDoUseMQ(Long blogId);

}
