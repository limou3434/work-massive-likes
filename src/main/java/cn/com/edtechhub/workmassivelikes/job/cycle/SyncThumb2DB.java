package cn.com.edtechhub.workmassivelikes.job.cycle;

import cn.com.edtechhub.workmassivelikes.enums.ThumbTypeEnum;
import cn.com.edtechhub.workmassivelikes.mapper.BlogMapper;
import cn.com.edtechhub.workmassivelikes.model.entity.Thumb;
import cn.com.edtechhub.workmassivelikes.service.ThumbService;
import cn.com.edtechhub.workmassivelikes.utils.RedisKeyUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.text.StrPool;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * 定时将 Redis 中的临时点赞记录同步到 MySQL 中
 */
@Component
@Slf4j
public class SyncThumb2DB {

    /**
     * 注入点赞服务依赖
     */
    @Resource
    private ThumbService thumbService;

    /**
     * 注入博文服务依赖
     */
    @Resource
    private BlogMapper blogMapper;

    /**
     * 注入 Redis 客户端
     */
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 每隔 10 秒执行一次备份(延迟备份)
     */
    @Scheduled(fixedRate = 10000)
    @Transactional(rollbackFor = Exception.class) // 把所有异常捕获然后回退事务
    public void run() {
        log.debug("Redis 点赞记录同步备份开始");
        DateTime nowDate = DateUtil.date();
        // 如果此时秒数为 0~9 则回到上一分钟的 50 秒
        int second = (DateUtil.second(nowDate) / 10 - 1) * 10;
        if (second == -10) {
            second = 50;
            // 回到上一分钟
            nowDate = DateUtil.offsetMinute(nowDate, -1);
        }
        String date = DateUtil.format(nowDate, "HH:mm:") + second;
        // 10:01:01 -> 10:00:50
        // 10:01:08 -> 10:00:50
        // 10:01:12 -> 10:00:10
        // 10:01:14 -> 10:00:10
        // 10:01:23 -> 10:00:20
        syncThumb2DBByDate(date); // 开始同步
        log.debug("Redis 点赞记录同步备份完成");
    }

    /**
     * 同步 Redis 中的点赞记录到数据库, data 为外部传递进来的临时点赞键名, 格式为 "HH:mm:ss"
     */
    public void syncThumb2DBByDate(String date) {
        // 获取到临时点赞记录
        String tempThumbKey = RedisKeyUtil.getTempThumbKey(date); // key(time_slice): "field(user_id:blog_id)=value(is_thumb)"
        Map<Object, Object> allTempThumbMap = redisTemplate
                .opsForHash()
                .entries(tempThumbKey); // 把 hash 中的所有 "字段 <-> 值" 都放入到 Map 中
        boolean thumbMapEmpty = CollUtil.isEmpty(allTempThumbMap);

        if (thumbMapEmpty) {
            return;
        }

        // 同步点赞到数据库
        Map<Long, Long> blogThumbCountMap = new HashMap<>();
        ArrayList<Thumb> thumbList = new ArrayList<>();
        LambdaQueryWrapper<Thumb> wrapper = new LambdaQueryWrapper<>();
        boolean needRemove = false;
        // key(time_slice): "field(user_id:blog_id)=value(is_thumb)"
        for (Object userIdAndBlogIdObj : allTempThumbMap.keySet()) { // 遍历所有的点赞记录
            String userIdAndBlogId = (String) userIdAndBlogIdObj;
            String[] userIdAndBlogIdList = userIdAndBlogId.split(StrPool.COLON);
            Long userId = Long.valueOf(userIdAndBlogIdList[0]); // user_id
            Long blogId = Long.valueOf(userIdAndBlogIdList[1]); // blog_id

            Integer thumbType = Integer.valueOf(allTempThumbMap.get(userIdAndBlogId).toString()); // is_thumb
            if (thumbType == ThumbTypeEnum.INCR.getValue()) {
                Thumb thumb = new Thumb();
                thumb.setUserId(userId);
                thumb.setBlogId(blogId);
                thumbList.add(thumb);
                log.debug("Redis 点赞记录将增加 +: 用户标识 {}, 博文标识 {}, 点赞类型 {}", userId, blogId,thumbType);
            } else if (thumbType == ThumbTypeEnum.DECR.getValue()) {
                // 拼接查询条件批量删除
                needRemove = true;
                wrapper.or().eq(Thumb::getUserId, userId).eq(Thumb::getBlogId, blogId); // 累积所有需删除的记录
                log.debug("Redis 点赞记录将减少 -: 用户标识 {}, 博文标识 {}, 点赞类型 {}", userId, blogId,thumbType);
            } else {
                if (thumbType == ThumbTypeEnum.NON.getValue()) {
                    log.debug("Redis 点赞记录没变化 o: 用户标识 {}, 博文标识 {}, 点赞类型 {}", userId, blogId,thumbType);
                } else {
                    log.debug("Redis 点赞记录出异常 x: 用户标识 {}, 博文标识 {}, 点赞类型 {}", userId, blogId,thumbType);
                }
                continue;
            }

            // 计算点赞增量
            blogThumbCountMap.put(
                    blogId,
                    blogThumbCountMap.getOrDefault(blogId, 0L) + thumbType // 如果之前有就让点赞次数更新
            );
        }

        // 批量插入
        thumbService.saveBatch(thumbList);

        // 批量删除
        if (needRemove) {
            thumbService.remove(wrapper);
        }

        // 批量更新博客点赞量
        if (!blogThumbCountMap.isEmpty()) {
            blogMapper.batchUpdateThumbCount(blogThumbCountMap);
        }

        // 异步删除
        Thread.startVirtualThread(() -> {
            redisTemplate.delete(tempThumbKey);
        });
    }

}
