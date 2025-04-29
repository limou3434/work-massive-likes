package cn.com.edtechhub.workmassivelikes.job.cycle;

import cn.com.edtechhub.workmassivelikes.contant.ThumbConstant;
import cn.com.edtechhub.workmassivelikes.model.dto.ThumbEventDto;
import cn.com.edtechhub.workmassivelikes.model.entity.Thumb;
import cn.com.edtechhub.workmassivelikes.service.ThumbService;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.shade.javax.annotation.Resource;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.pulsar.core.PulsarTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 定时将 MQ 中的消息同步到 MySQL 中的补偿措施
 */
@Component
@Slf4j
public class ThumbReconcile {

    /**
     * 注入 Redis 客户端
     */
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 注入点赞服务依赖
     */
    @Resource
    private ThumbService thumbService;

    /**
     * 注入 Pulsar 客户端
     */
    @Resource
    private PulsarTemplate<ThumbEventDto> pulsarTemplate;

    /**
     * 定时任务入口
     */
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨 2 点执行一次
    public void run() {
        log.debug("开始补偿临时数据");
        long startTime = System.currentTimeMillis();

        Set<Long> userIds = new HashSet<>(); // 用于记录所有在 Redis 中点过赞用户的 id
        String pattern = ThumbConstant.USER_THUMB_KEY_PREFIX + "*";
        try (Cursor<String> cursor = redisTemplate.scan(
                ScanOptions
                        .scanOptions()
                        .match(pattern) // 设置匹配规则
                        .count(1000) // 建议 Redis 每次返回最多 1000 个 key
                        .build()) // 最终只得到 key 名的列表
        ) { // 从 Redis 中 scan 出所有匹配的 key 并提取 userId
            while (cursor.hasNext()) {
                String key = cursor.next();
                Long userId = Long.valueOf(key.replace(ThumbConstant.USER_THUMB_KEY_PREFIX, "")); // 从 key 中去掉 USER_THUMB_KEY_PREFIX 前缀(其实就是用 "" 替换掉前缀)
                userIds.add(userId);
            }
        }

        // 逐用户比对
        userIds.forEach(userId -> {
            Set<Long> redisBlogIds = redisTemplate.opsForHash().keys(ThumbConstant.USER_THUMB_KEY_PREFIX + userId).stream().map(obj -> Long.valueOf(obj.toString())).collect(Collectors.toSet()); // 获取该用户在 Redis 中的所有点赞记录
            Set<Long> mysqlBlogIds = Optional.ofNullable(
                            thumbService
                                    .lambdaQuery()
                                    .eq(Thumb::getUserId, userId)
                                    .list()
                    )
                    .orElse(new ArrayList<>())
                    .stream()
                    .map(Thumb::getBlogId)
                    .collect(Collectors.toSet()); // 获取该用户在 MySQL 中的所有点赞记录

            // 计算差异
            Set<Long> diffBlogIds = Sets.difference(redisBlogIds, mysqlBlogIds);

            // 发送补偿事件
            sendCompensationEvents(userId, diffBlogIds);
        });

        log.debug("对账任务完成, 耗时 {} ms", System.currentTimeMillis() - startTime);
    }

    /**
     * 发送补偿事件到 Pulsar(把 Redis 中缺失的消息再次补偿出去)
     */
    private void sendCompensationEvents(Long userId, Set<Long> blogIds) {
        blogIds.forEach(blogId -> {
            ThumbEventDto thumbEvent = new ThumbEventDto(userId, blogId, ThumbEventDto.EventType.INCR, LocalDateTime.now());
            pulsarTemplate
                    .sendAsync("thumb-topic", thumbEvent)
                    .exceptionally(ex -> {
                        log.debug("补偿事件发送失败: userId = {}, blogId = {}", userId, blogId);
                        return null;
                    });
        });
    }

}

