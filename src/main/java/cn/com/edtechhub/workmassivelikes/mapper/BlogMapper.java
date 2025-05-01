package cn.com.edtechhub.workmassivelikes.mapper;

import cn.com.edtechhub.workmassivelikes.model.entity.Blog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

/**
 * 博客映射
 *
 * @author <a href="https://github.com/limou3434">limou3434</a>
 */
public interface BlogMapper extends BaseMapper<Blog> {

    void batchUpdateThumbCount(@Param("countMap") Map<Long, Long> countMap); // 需要在对应的 Mapper.xml 文件中添加自定义的 SQL 来改动点赞总数

}




