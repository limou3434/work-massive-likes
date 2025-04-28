package cn.com.edtechhub.workmassivelikes.mapper;

import cn.com.edtechhub.workmassivelikes.model.entity.Blog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

/**
* @author Limou
* @description 针对表【blog(博客表)】的数据库操作Mapper
* @createDate 2025-04-23 12:30:45
* @Entity cn.com.edtechhub.workmassivelikes.model.entity.Blog
*/
public interface BlogMapper extends BaseMapper<Blog> {

    void batchUpdateThumbCount(@Param("countMap") Map<Long, Long> countMap); // 需要在对应的 Mapper.xml 文件中添加自定义的 SQL 来改动点赞总数

}




