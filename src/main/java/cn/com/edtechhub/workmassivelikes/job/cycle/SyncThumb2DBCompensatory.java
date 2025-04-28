package cn.com.edtechhub.workmassivelikes.job.cycle;

import cn.com.edtechhub.workmassivelikes.contant.ThumbConstant;
import cn.com.edtechhub.workmassivelikes.utils.RedisKeyUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * 定时将 Redis 中的临时点赞记录同步到 MySQL 中的补偿措施
 */
@Component
@Slf4j
public class SyncThumb2DBCompensatory {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private SyncThumb2DB syncThumb2DBJob;

    @Scheduled(cron = "0 0 2 * * *") // 每天凌晨 2 点执行一次
    public void run() {
        log.debug("开始补偿临时数据");
        Set<String> thumbKeys = redisTemplate.keys(RedisKeyUtil.getTempThumbKey("") + "*"); // 获取所有临时点赞记录, 也就是 thumb:temp:%s*
        Set<String> needHandleDataSet = new HashSet<>();
        thumbKeys
                .stream()
                .filter(ObjUtil::isNotNull) // 过滤掉为 null 的 key
                .forEach(
                        thumbKey -> needHandleDataSet.add(
                                thumbKey.replace(ThumbConstant.TEMP_THUMB_KEY_PREFIX.formatted(""), "") // 从每个 key 中去掉 TEMP_THUMB_KEY_PREFIX 前缀
                        )
                );

        if (CollUtil.isEmpty(needHandleDataSet)) {
            log.debug("无需补偿临时数据");
            return;
        }

        // 补偿数据
        for (String date : needHandleDataSet) {
            syncThumb2DBJob.syncThumb2DBByDate(date);
        }
        log.debug("完成补偿临时数据");
    }
}
