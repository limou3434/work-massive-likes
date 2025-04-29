package cn.com.edtechhub.workmassivelikes.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class CacheManager {

    @Bean
    public HeavyKeeper heavyKeeper() {
        return new HeavyKeeper(100, 100000, 5, 0.92, 10);
    }

    @Bean
    public Cache<String, Object> localCache() {
        return Caffeine.newBuilder().maximumSize(1000).expireAfterWrite(5, TimeUnit.MINUTES).build();
    }

}
