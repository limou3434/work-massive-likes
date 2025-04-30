package cn.com.edtechhub.workmassivelikes.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 点赞事件 DTO
 *
 * @author <a href="https://github.com/limou3434">limou3434</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThumbEventDto implements Serializable {

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 博客 ID
     */
    private Long blogId;

    /**
     * 事件类型
     */
    private EventType type;

    /**
     * 事件发生时间
     */
    private LocalDateTime eventTime;

    /**
     * 事件类型枚举
     */
    public enum EventType {
        /**
         * 点赞
         */
        INCR,

        /**
         * 取消点赞
         */
        DECR
    }
}
