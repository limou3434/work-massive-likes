package cn.com.edtechhub.workmassivelikes.service;

import cn.com.edtechhub.workmassivelikes.model.entity.Thumb;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @author Limou
 * @description 针对表【thumb(点赞表)】的数据库操作Service
 * @createDate 2025-04-23 12:30:45
 */
public interface ThumbService extends IService<Thumb> {

    /**
     * 确认点赞
     */
    Boolean thumbAddDo(Long blogId);

    /**
     * 取消点赞
     */
    Boolean thumbAddUnDo(Long blogId);

}
