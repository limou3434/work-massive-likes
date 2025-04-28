package cn.com.edtechhub.workmassivelikes.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 缓存类
 */
@Component
@Slf4j
public class CacheManager {

    private TopK hotKeyDetector;

    private Cache<String, Object> localCache;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 获取 TopK 数据结构
     */
    @Bean
    public TopK getHotKeyDetector() {
        hotKeyDetector = new HeavyKeeper(
                // 监控 Top 100 Key
                100,
                // 哈希表宽度
                100000,
                // 哈希表深度
                5,
                // 衰减系数
                0.92,
                // 最小出现 10 次才记录
                10
        );
        return hotKeyDetector;
    }

    /**
     * 获取本地缓存
     */
    @Bean
    public Cache<String, Object> localCache() {
        return localCache = Caffeine
                .newBuilder() // 开始建一个新的缓存对象
                .maximumSize(1000) // 缓存最多只存 1000 个元素, 超过了会根据访问频率自动淘汰旧数据(LRU 淘汰策略)
                .expireAfterWrite(5, TimeUnit.MINUTES) // 一个 key 写入后, 5 分钟后自动失效, 无论有没有被访问
                .build();
    }

    /**
     * 构造复合 key
     */
    private String buildCacheKey(String hashKey, String key) {
        return hashKey + ":" + key;
    }

    public Object get(String hashKey, String key) {
        // 构造唯一的 composite key
        String compositeKey = buildCacheKey(hashKey, key);

        // 1. 先查本地缓存
        Object value = localCache.getIfPresent(compositeKey);
        if (value != null) {
            log.info("CAffine 缓存获取到数据 {} = {}", compositeKey, value);
            // 记录访问次数（每次访问计数 +1）
            hotKeyDetector.add(key, 1);
            return value;
        }

        // 2. 本地缓存未命中, 查询 Redis
        Object redisValue = redisTemplate.opsForHash().get(hashKey, key);
        if (redisValue == null) {
            return null;
        }
        log.info("Redis 缓存获取到数据 {} = {}", compositeKey, redisValue);

        // 3. 记录访问(计数 +1)
        AddResult addResult = hotKeyDetector.add(key, 1);

        // 4. 如果是热 Key 且不在本地缓存，则缓存数据
        if (addResult.isHotKey()) {
            localCache.put(compositeKey, redisValue);
        }

        return redisValue;
    }

    /**
     * 如果 key 在本地缓存中, 则更新 value
     */
    public void putIfPresent(String hashKey, String key, Object value) {
        String compositeKey = buildCacheKey(hashKey, key);
        Object object = localCache.getIfPresent(compositeKey);
        if (object == null) {
            return;
        }
        localCache.put(compositeKey, value);
    }

    /**
     * 定时清理过期的热 Key 检测数据
     */
    @Scheduled(fixedRate = 20, timeUnit = TimeUnit.SECONDS)
    public void cleanHotKeys() {
        hotKeyDetector.fading();
    }

}
