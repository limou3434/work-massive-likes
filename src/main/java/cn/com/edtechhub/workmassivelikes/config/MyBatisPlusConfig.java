package cn.com.edtechhub.workmassivelikes.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import lombok.Data;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis Plus 配置
 *
 * @author <a href="https://github.com/limou3434">limou3434</a>
 */
@Configuration
@MapperScan("cn.com.edtechhub.workmassivelikes.mapper") // 启用 MyBatisPlus 扫描 ./src/Mapper/ 中的映射
@Data
public class MyBatisPlusConfig {

    /**
     * 默认读取项目名称作为盐值, 如果需要安全则可以注释掉注解并且直接赋值
     */
    @Value("${mybatis-plus.salt}")
    String salt;

    /**
     * 添加分页插件
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL)); // 如果配置多个插件, 切记分页最后添加
        // 如果有多数据源可以不配具体类型, 否则都建议配上具体的 DbType
        return interceptor;
    }
}
