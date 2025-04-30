package cn.com.edtechhub.workmassivelikes.contant;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 应用项目常量
 *
 * @author <a href="https://github.com/limou3434">limou3434</a>
 */
@Configuration
@Data
public class ServerConstant {

    /**
     * 项目名称
     */
    @Value("${spring.application.name}")
    private String projectName;

    /**
     * 运行地址
     */
    @Value("${server.address}")
    private String address;

    /**
     * 运行端口
     */
    @Value("${server.port}")
    private String port;

    /**
     * 接口前缀
     */
    @Value("${spring.mvc.servlet.path}")
    private String contextPath;

}
