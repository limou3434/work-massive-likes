package cn.com.edtechhub.workmassivelikes.service;

import cn.com.edtechhub.workmassivelikes.model.dto.BlogDto;
import cn.com.edtechhub.workmassivelikes.model.entity.Blog;
import cn.com.edtechhub.workmassivelikes.request.*;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author Limou
* @description 针对表【blog(博客表)】的数据库操作Service
* @createDate 2025-04-23 12:30:45
*/
public interface BlogService extends IService<Blog> {

    /**
     * 查询博文
     */
    List<Blog> blogSearch(BlogSearchRequest blogSearchRequest);

    /**
     * 查询博文(包含当前用户点赞情况)
     */
    List<BlogDto> blogSearchIncludeHasThumb(BlogSearchRequest blogSearchRequest);

    /**
     * 查询博文(根据博文 id 查询, 并且包含当前用户点赞情况)
     */
    List<BlogDto> blogSearchIncludeHasThumbById(Long blogId);

}
