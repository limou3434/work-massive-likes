package cn.com.edtechhub.workmassivelikes.service;

import cn.com.edtechhub.workmassivelikes.model.dto.BlogDto;
import cn.com.edtechhub.workmassivelikes.model.entity.Blog;
import cn.com.edtechhub.workmassivelikes.request.*;
import com.baomidou.mybatisplus.extension.service.IService;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 博客服务声明
 *
 * @author <a href="https://github.com/limou3434">limou3434</a>
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
