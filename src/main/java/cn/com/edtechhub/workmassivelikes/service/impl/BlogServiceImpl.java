package cn.com.edtechhub.workmassivelikes.service.impl;

import cn.com.edtechhub.workmassivelikes.contant.ThumbConstant;
import cn.com.edtechhub.workmassivelikes.mapper.BlogMapper;
import cn.com.edtechhub.workmassivelikes.model.dto.BlogDto;
import cn.com.edtechhub.workmassivelikes.model.entity.Blog;
import cn.com.edtechhub.workmassivelikes.model.entity.Thumb;
import cn.com.edtechhub.workmassivelikes.request.BlogSearchRequest;
import cn.com.edtechhub.workmassivelikes.service.BlogService;
import cn.com.edtechhub.workmassivelikes.service.ThumbService;
import cn.com.edtechhub.workmassivelikes.service.UserService;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Limou
 * @description 针对表【blog(博客表)】的数据库操作Service实现
 * @createDate 2025-04-23 12:30:45
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements BlogService {

    @Resource
    UserService userService;

    @Resource
    @Lazy // 由于需要在 thumbService 内部依赖 blogService, 因此懒加载避免循环依赖
    ThumbService thumbService;

    /**
     * 注入 Redis 客户端
     */
    @Resource
    RedisTemplate<String, Object> redisTemplate;

    @Override
    public List<Blog> blogSearch(BlogSearchRequest blogSearchRequest) {
        // 先获取到数据中的所有博文记录
        Page<Blog> page = new Page<>(blogSearchRequest.getPageCurrent(), blogSearchRequest.getPageSize()); // 创建分页对象, 指定页码和每页条数
        LambdaQueryWrapper<Blog> queryWrapper = this.getQueryWrapper(blogSearchRequest); // 构造查询条件
        Page<Blog> blogPage = this.page(page, queryWrapper); // 调用 MyBatis-Plus 的分页查询方法
        return blogPage.getRecords(); // 返回分页结果
    }

    @Override
    public List<BlogDto> blogSearchIncludeHasThumb(BlogSearchRequest blogSearchRequest) {
        List<Blog> blogList = this.blogSearch(blogSearchRequest);

        // 查询当前用户对所有文章列表的点赞情况(提前从数据中获取所有的点赞记录, 避免每篇文章都需要遍历查询一次)
        Map<Long, Boolean> blogIdHasThumbMap = new HashMap<>(); // 由博文 id 和当前用户是否点赞组成

//        Set<Long> blogIdSet = blogList
//                .stream()
//                .map(Blog::getId)
//                .collect(Collectors.toSet()); // 获取博文 id 集合
//
//        List<Thumb> thumbList = thumbService.lambdaQuery()
//                .eq(Thumb::getUserId, Long.valueOf(userService.userStatus().getUserId()))
//                .in(Thumb::getBlogId, blogIdSet) // 在博文 id 集合中查询
//                .list(); // 最终过滤得到和当前用户关联的点赞列表
//
//        thumbList.forEach(thumb -> blogIdHasThumbMap.put(thumb.getBlogId(), true)); // 最终获取到博文 id 和当前用户对对应文章是否点赞的 map

        // 改为在 Redis 中进行点赞情况查询
        List<Object> blogIdList = blogList
                .stream()
                .map(blog -> blog.getId().toString())
                .collect(Collectors.toList()); // 先获取当前用户的对应键值对 hash 的列表

        List<Object> thumbList = redisTemplate.opsForHash().multiGet( // multiGet 即是对 hMGet 命令的封装
                ThumbConstant.USER_THUMB_KEY_PREFIX + userService.userStatus().getUserId(), // 键名
                blogIdList // 需要查询的 field 组成的列表
        ); // 到这里就获取到 Redis 中的当前用户对于所有的博客的点赞情况

        for (int i = 0; i < thumbList.size(); i++) {
            if (thumbList.get(i) == null) {
                continue; // 这里其实也可以选择设置为 false, 不过为了节约空间, 选择不理会也是值得的
            }
            blogIdHasThumbMap.put(Long.valueOf(blogIdList.get(i).toString()), true);
        }

        return blogList
                .stream()
                .map(blog -> {
                    BlogDto blogDto = BeanUtil.copyProperties(blog, BlogDto.class);
                    blogDto.setHasThumb(blogIdHasThumbMap.get(blog.getId()));
                    return blogDto;
                })
                .toList()
                ;
    }

    @Override
    public List<BlogDto> blogSearchIncludeHasThumbById(Long blogId) {
        BlogSearchRequest blogSearchRequest = new BlogSearchRequest();
        blogSearchRequest.setId(blogId);
        return this.blogSearchIncludeHasThumb(blogSearchRequest);
    }

    /**
     * 获取查询封装器的方法
     */
    private LambdaQueryWrapper<Blog> getQueryWrapper(BlogSearchRequest blogSearchRequest) {
        // 取得需要查询的参数
        Long id = blogSearchRequest.getId();
        Long userId = blogSearchRequest.getUserId();
        String title = blogSearchRequest.getTitle();
        String coverImg = blogSearchRequest.getCoverImg();
        String content = blogSearchRequest.getContent();
        Integer thumbCount = blogSearchRequest.getThumbCount();
        String sortOrder = blogSearchRequest.getSortOrder();
        String sortField = blogSearchRequest.getSortField();

        // 获取包装器进行返回
        LambdaQueryWrapper<Blog> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(id != null, Blog::getId, id);
        lambdaQueryWrapper.eq(userId != null, Blog::getUserId, userId);
        lambdaQueryWrapper.eq(StringUtils.isNotBlank(title), Blog::getTitle, title);
        lambdaQueryWrapper.eq(StringUtils.isNotBlank(coverImg), Blog::getCoverImg, coverImg);
        lambdaQueryWrapper.eq(StringUtils.isNotBlank(content), Blog::getContent, content);
        lambdaQueryWrapper.eq(thumbCount != null, Blog::getThumbCount, thumbCount);
        lambdaQueryWrapper.orderBy(
                StringUtils.isNotBlank(sortField) && !StringUtils.containsAny(sortField, "=", "(", ")", " "),
                sortOrder.equals("ascend"), // 这里结果为 true 代表 ASC 升序, false 代表 DESC 降序
                Blog::getTitle // 默认按照标题排序
        );
        return lambdaQueryWrapper;
    }

}




