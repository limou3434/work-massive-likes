package cn.com.edtechhub.workmassivelikes.pulsar;

import cn.com.edtechhub.workmassivelikes.mapper.BlogMapper;
import cn.com.edtechhub.workmassivelikes.model.dto.ThumbEventDto;
import cn.com.edtechhub.workmassivelikes.model.entity.Thumb;
import cn.com.edtechhub.workmassivelikes.service.ThumbService;
import cn.hutool.core.lang.Pair;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.common.schema.SchemaType;
import org.springframework.pulsar.annotation.PulsarListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service // Spring 自动注册为 Bean
@Slf4j
public class ThumbConsumer {

    /**
     * 注入博文映射
     */
    @Resource
    private BlogMapper blogMapper;

    /**
     * 注入点赞服务
     */
    @Resource
    private ThumbService thumbService;

    /**
     * 批量处理监听器(消费者)
     */
    @PulsarListener(
            subscriptionName = "thumb-subscription", // 订阅名/消费者组
            topics = "thumb-topic", // 要消费的主题
            schemaType = SchemaType.JSON, // 数据格式
            batch = true, // 开启批量消费
            consumerCustomizer = "pulsarConfig" // 自定义消费者配置(批量消息消费逻辑, 自动对所有 messages 调用 ack() 告诉 Pulsar Broker 已经消费完毕, 移除消息队列中被消费的消息)
//            negativeAckRedeliveryBackoff = "negativeAckRedeliveryBackoff" // 引用失败重试策略(有些问题需要查阅文档)
//            ackTimeoutRedeliveryBackoff = "ackTimeoutRedeliveryBackoff", // 引用超时重试策略(有些问题需要查阅文档)
//            deadLetterPolicy = "deadLetterPolicy" // 引用死信队列策略
    ) // 批量消费监听器
    @Transactional(rollbackFor = Exception.class) // 开启事务
    public void processBatch(List<Message<ThumbEventDto>> messages) throws Exception {
        // 打印消息数量
        log.debug("需要消费的消息个数: {}", messages.size());

        // 提取事件并过滤无效消息
        List<ThumbEventDto> events = messages
                .stream()
                .map(Message::getValue) // 提取每条消息的内容得到 ThumbEventDto
                .filter(Objects::nonNull) // 过滤无效消息(可能是空消息或反序列化失败)
                .toList();

        log.debug("提取事件并过滤无效消息: {}", events);

        // 按 (userId, blogId) 分组, 并获取每个分组的最新事件
        Map<Pair<Long, Long>, ThumbEventDto> latestEvents = events
                .stream()
                .collect( // 把流转化为其他数据类型
                        Collectors.groupingBy(e -> Pair.of(e.getUserId(), e.getBlogId()), // 按 (userId, blogId) 分组
                                Collectors.collectingAndThen(Collectors.toList(), list -> { // 先用一个收集器收集前面的分组数据, 然后再对收集结果做进一步处理
                                    list.sort(Comparator.comparing(ThumbEventDto::getEventTime)); // 组内按时间升序
                                    return list.size() % 2 == 0 ? null : list.getLast(); // 组内如果是偶数个则返回 null(说明这个某个用户对某篇博文进行了多次的操作, 由于 Lua 脚本已经提前处理重复确认点赞和重复取消点赞的情况, 因此只会剩下例如 "确认+取消(无效)、取消+确认(无效)", 这种情况直接不需要让 MySQL 处理
                                }))
                ); // 最终得到分组结果

        List<Thumb> thumbs = new ArrayList<>(); // 用于记录每个点赞记录
        Map<Long, Long> countMap = new ConcurrentHashMap<>(); // 用于记录每篇博文的点赞数量
        AtomicReference<Boolean> needRemove = new AtomicReference<>(false); // 用于记录是否需要删除数据库中的点赞记录, 这是一个原子类型, 这个变量是个标记位

        LambdaQueryWrapper<Thumb> wrapper = new LambdaQueryWrapper<>();

        log.debug("分组结果: {}", latestEvents);

        // 遍历分组结果
        latestEvents.forEach((userAndBlogPair, event) -> {
            // 如果分组结果为空, 则结束处理逻辑
            if (event == null) {
                return;
            }
            // 如果分组结果非空, 则判断事件类型
            else {
                ThumbEventDto.EventType finalAction = event.getType();

                // 如果是确认点赞
                if (finalAction == ThumbEventDto.EventType.INCR) {
                    // 创建一个点赞记录并且添加到点赞记录列表中
                    Thumb thumb = new Thumb();
                    thumb.setBlogId(event.getBlogId());
                    thumb.setUserId(event.getUserId());
                    thumbs.add(thumb);

                    // 更新点赞数量
                    countMap.merge(event.getBlogId(), 1L, Long::sum);
                }
                // 如果是取消点赞
                else if (finalAction == ThumbEventDto.EventType.DECR) {
                    // 设置需要删除记录的标志同时设置需要删除记录的查询条件
                    needRemove.set(true); // 标记, 代表等下持久化时需要删除数据库中的点赞记录
                    wrapper
                            .or()
                            .eq(Thumb::getUserId, event.getUserId())
                            .eq(Thumb::getBlogId, event.getBlogId())
                    ;

                    // 更新点赞数量
                    countMap.merge(event.getBlogId(), -1L, Long::sum);
                } else {
                    log.debug("未知的事件类型, 需要紧急处理: {}", finalAction);
                }
            }
        });

        log.debug("需要消费的消息个数: {}, 需要插入的点赞记录: {}, 需要更新的点赞情况 {}, 是否设置了需要删除数据库中的点赞记录的标记: {}", messages.size(), thumbs.size(), countMap, needRemove.get());

        // 批量更新数据库
        if (needRemove.get()) {
            log.debug("删除数据库中的点赞记录有: {}", thumbService.list(wrapper));
            thumbService.remove(wrapper);
        }
        batchInsertThumbs(thumbs);
        batchUpdateBlogs(countMap);
    }

//    /**
//     * 死信队列监听器(消费者)
//     */
//    @PulsarListener(topics = "thumb-dlq-topic")
//    public void consumerDlq(Message<ThumbEventDto> message) {
//        MessageId messageId = message.getMessageId();
//        log.info("异常消息 {} 入库, 通知相关人员 {} 处理", messageId, "898738804@qq.com");
//    }
//
//    /**
//     * 配置 ACK 失败重试策略
//     */
//    @Bean
//    @Lazy
//    public RedeliveryBackoff negativeAckRedeliveryBackoff() {
//        log.debug("ACK 失败重试策略被启用");
//        return MultiplierRedeliveryBackoff.builder()
//                .minDelayMs(1000) // 初始延迟 1 秒
//                .maxDelayMs(60_000) // 最大延迟 60 秒
//                .multiplier(2) // 每次重试延迟倍数
//                .build();
//    }
//
//    /**
//     * 配置 ACK 超时重试策略
//     */
//    @Bean
//    @Lazy
//    public RedeliveryBackoff ackTimeoutRedeliveryBackoff() {
//        log.debug("ACK 超时重试策略被启用");
//        return MultiplierRedeliveryBackoff.builder()
//                .minDelayMs(5000) // 初始延迟 5 秒
//                .maxDelayMs(300_000) // 最大延迟 300 秒
//                .multiplier(3) // 每次重试延迟倍数
//                .build();
//    }
//
//    /**
//     * 配置死信队列
//     */
//    @Bean
//    @Lazy
//    public DeadLetterPolicy deadLetterPolicy() {
//        return DeadLetterPolicy.builder()
//                .maxRedeliverCount(3) // 最大重试次数
//                .deadLetterTopic("thumb-dlq-topic") // 死信主题名称
//                .build();
//    }

    /**
     * 批量插入点赞记录
     */
    public void batchInsertThumbs(List<Thumb> thumbs) {
        if (!thumbs.isEmpty()) {
            thumbService.saveBatch(thumbs, 500);
        }
    }

    /**
     * 批量更新博文点赞数量
     */
    public void batchUpdateBlogs(Map<Long, Long> countMap) {
        if (!countMap.isEmpty()) {
            blogMapper.batchUpdateThumbCount(countMap);
        }
    }

}

