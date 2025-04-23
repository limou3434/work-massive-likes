package cn.com.edtechhub.workmassivelikes.controller;

import cn.com.edtechhub.workmassivelikes.enums.CodeBindMessage;
import cn.com.edtechhub.workmassivelikes.model.dto.BlogDto;
import cn.com.edtechhub.workmassivelikes.model.entity.Blog;
import cn.com.edtechhub.workmassivelikes.request.BlogSearchRequest;
import cn.com.edtechhub.workmassivelikes.response.BaseResponse;
import cn.com.edtechhub.workmassivelikes.response.TheResult;
import cn.com.edtechhub.workmassivelikes.service.BlogService;
import cn.com.edtechhub.workmassivelikes.service.ThumbService;
import cn.com.edtechhub.workmassivelikes.service.UserService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController // 返回值默认为 json 类型
@RequestMapping("/blog")
@Slf4j
public class BlogController {

    @Resource
    private BlogService blogService;

    @Lazy // 解决循环引用问题, 因为后续 ThumbService 的开发也需要引入 UserService 作为依赖
    @Resource
    private ThumbService thumbService;

    @Resource
    private UserService userService;

    /**
     * 查询博文
     */
    @SaCheckLogin
    @SaCheckRole("admin")
    @PostMapping("/search")
    public BaseResponse<List<Blog>> blogSearch(@RequestBody BlogSearchRequest blogSearchRequest) {
        List<Blog> blogList = blogService.blogSearch(blogSearchRequest);
        return TheResult.success(CodeBindMessage.SUCCESS, blogList);
    }

    /**
     * 查询博客(包含当前用户点赞情况)
     */
    @SaCheckLogin
    @PostMapping("/search/include_has_thumb")
    public BaseResponse<List<BlogDto>> blogSearchIncludeHasThumb(@RequestBody BlogSearchRequest blogSearchRequest) {
        List<BlogDto> blogDtoList = blogService.blogSearchIncludeHasThumb(blogSearchRequest);
        return TheResult.success(CodeBindMessage.SUCCESS, blogDtoList);
    }

    /**
     * 查询博文(根据博文 id 查询, 并且包含当前用户点赞情况)
     */
    @SaCheckLogin
    @PostMapping("/search/include_has_thumb/by/id")
    public BaseResponse<List<BlogDto>> blogSearchIncludeHasThumbById(Long blogId) {
        List<BlogDto> blogDtoList = blogService.blogSearchIncludeHasThumbById(blogId);
        return TheResult.success(CodeBindMessage.SUCCESS, blogDtoList);
    }

}
