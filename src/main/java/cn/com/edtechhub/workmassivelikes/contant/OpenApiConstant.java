package cn.com.edtechhub.workmassivelikes.contant;

import lombok.Data;
import org.springframework.context.annotation.Configuration;

/**
 * OpenApi 常量
 *
 * @author <a href="https://github.com/limou3434">limou3434</a>
 */
@Configuration
@Data
public class OpenApiConstant {

    /**
     * Knife4j Ui 在线文档地址
     */
    private String knife4jUi = "/doc.html";

    /**
     * Swagger Ui 在线文档地址
     */
    private String swaggerUi = "/swagger-ui/index.html";

    /**
     * 配置文档 json 地址
     */
    private String apiDocsJson = "/v3/api-docs";

}
