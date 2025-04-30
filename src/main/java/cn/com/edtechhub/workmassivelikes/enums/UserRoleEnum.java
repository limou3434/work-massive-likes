package cn.com.edtechhub.workmassivelikes.enums;

import lombok.Getter;

/**
 * 用户角色枚举体
 *
 * @author <a href="https://github.com/limou3434">limou3434</a>
 */
@Getter
public enum UserRoleEnum { // 由于效率问题, 这里手动缓存了数据库中的用户权限, 数据库中的用户等级只是用来限制插入和前端查询的

    /**
     * 封号角色枚举实例
     */
    BAN_ROLE(-1, "ban"),

    /**
     * 用户角色枚举实例
     */
    USER_ROLE(0, "user"),

    /**
     * 管理角色枚举实例
     */
    ADMIN_ROLE(1, "admin");

    /**
     * 角色码值
     */
    private final int code;

    /**
     * 角色描述
     */
    private final String description;

    /**
     * 内部角色构造方法
     *
     * @param code
     * @param description
     */
    UserRoleEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据角色码值获取橘色描述
     *
     * @param code
     * @return
     */
    public static String getUserDescription(int code) {
        for (UserRoleEnum role : UserRoleEnum.values()) {
            if (role.getCode() == code) {
                return role.getDescription();
            }
        }
        return null;
    }

}
