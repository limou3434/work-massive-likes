package cn.com.edtechhub.workmassivelikes.config;

import org.apache.pulsar.client.api.BatchReceivePolicy;
import org.apache.pulsar.client.api.ConsumerBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.pulsar.annotation.PulsarListenerConsumerBuilderCustomizer;

import java.util.concurrent.TimeUnit;

/**
 * Pulsar 配置
 *
 * @author <a href="https://github.com/limou3434">limou3434</a>
 */
@Configuration
public class PulsarConfig<T> implements PulsarListenerConsumerBuilderCustomizer<T> {

    @Override
    public void customize(ConsumerBuilder<T> consumerBuilder) {
        consumerBuilder.batchReceivePolicy(
                BatchReceivePolicy
                        .builder()
                        .maxNumMessages(1000) // 每次处理 1000 条
                        .timeout(10000, TimeUnit.MILLISECONDS) // 设置超时时间(单位毫秒)
                        .build()
        );
    }

}
