package cn.com.edtechhub.workmassivelikes.job.cycle;

import cn.com.edtechhub.workmassivelikes.cache.HeavyKeeper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class CleanCaffeineHotKeys {

    @Resource
    private HeavyKeeper hotKeyDetector;

    @Scheduled(fixedRate = 20, timeUnit = TimeUnit.SECONDS)
    public void cleanHotKeys() {
        log.debug("衰弱一次 TopK 数据结构");
        hotKeyDetector.fading();
    }

}
