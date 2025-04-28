package cn.com.edtechhub.workmassivelikes.enums;

import lombok.Getter;

/**
 * Lua 脚本执行状态
 */
@Getter
public enum LuaStatusEnum {

    /**
     * 执行成功
     */
    SUCCESS(1L),

    /**
     * 执行失败
     */
    FAIL(-1L),

    ;

    private final long value;

    LuaStatusEnum(long value) {
        this.value = value;
    }

}
