package cn.com.edtechhub.workmassivelikes.enums;

import lombok.Getter;

/**
 * 临时记录中的点赞类型
 */
@Getter
public enum ThumbTypeEnum {

    /**
     * 确认点赞
     */
    INCR(+1),

    /**
     * 取消点赞
     */
    DECR(-1),

    /**
     * 毫无改变
     */
    NON(0),
    ;

    private final int value;

    ThumbTypeEnum(int value) {
        this.value = value;
    }

}
