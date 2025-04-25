/**
 * 1. 生产配置托管在 Github 中
 * 2. [持久层]在 sql 中编写对应数据库文件并且设置假数据, 运行插入数据, 得到各个实体之间的关系, 使用 MyBatisPlus 导入 server、serverImpl、mapper
 * 3. [服务层]编写 serveice 接口声明和 serviceImpl 接口实现
 * (1) 尽可能不要使用 Request 作为参数但增删改查服务除外
 * (2) 编写简单的 CUDA 代码
 * a. 内部业务出错抛出异常 BusinessException(CodeBindMessage.xxxxx, "更加详细的错误说明"), 就可以让控制层响应异常给前端并且避免直接处理报文, 如果发现 CodeBindMessage 枚举不够使用可以自己定义
 * b. 外部组件出错抛出异常可以在全局异常拦截器 GlobalExceptionHandler 中捕获处理, 以达到自动让控制层响应异常给前端, 并且组件抛异常可以和本业务服务进行隔离
 * (3) 需要做 "复杂校验、业务处理、数据返回"
 * 4. [控制层]每个 controller 都需要有直接对应的 serviceImpl
 * (1) 做到 HTTP 层次上的 CUDA, 接口名和路由名保持强一致, 一是方便内部项目测试, 二是方便其他项目参考
 * (2) 默认只返回 200, 某些特殊的错误交给前端响应, 比如 403、404 以及对应的页面, 详细错误 code-message 在响应 JSON 中体现
 * (3) 需要做 "流量治理、用户认证、调用服务、报文处理、数据脱敏"
 * (4) controller 中编写业务上需要的复杂接口
 * 5. 将所有的实体服务通过 Dubbo 在 Nacos 中注册, 这样服务系统的微服务初步构成
 * 6. 将所有的实体服务通过 Dubbo 从 Nacos 中获取, 这样业务系统的可复用多个服务
 * 7. 因此从整个架构来看, 服务是共享的, 接口是独有的, 配置是托管的
 */

package cn.com.edtechhub.workmassivelikes;

import cn.com.edtechhub.workmassivelikes.config.ServerConfig;
import cn.com.edtechhub.workmassivelikes.config.OpenApiConfig;
import cn.dev33.satoken.SaManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 启动类
 *
 * @author <a href="https://github.com/limou3434">limou3434</a>
 */
@SpringBootApplication
@Slf4j
public class WorkMassiveLikesApplication {

    public static void main(String[] args) {
        var context = SpringApplication.run(WorkMassiveLikesApplication.class, args);
        ServerConfig serverConfig = context.getBean(ServerConfig.class);
        OpenApiConfig springdocConfig = context.getBean(OpenApiConfig.class);
        String baseUrl = "http://" + serverConfig.getAddress() + ":" + serverConfig.getPort() + serverConfig.getContextPath();
        log.info("Spring Boot running...");
        log.info("访问 {} 或 {} 即可得到在线文档, 访问 {} 即可得到文档配置", baseUrl + springdocConfig.getKnife4jUi(), baseUrl + springdocConfig.getSwaggerUi(), baseUrl + springdocConfig.getApiDocsJson());
        log.debug("读取 Sa-token 配置查验是否正确: {}", SaManager.getConfig());
        log.debug("读取 Sa-token 切面类查验是否被替换为自己的: {}", SaManager.getStpInterface());
    }

}
