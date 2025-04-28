package cn.com.edtechhub.workmassivelikes.service;

import cn.com.edtechhub.workmassivelikes.model.entity.Thumb;
import com.baomidou.mybatisplus.extension.service.IService;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

/**
 * @author Limou
 * @description 针对表【thumb(点赞表)】的数据库操作Service
 * @createDate 2025-04-23 12:30:45
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

}
