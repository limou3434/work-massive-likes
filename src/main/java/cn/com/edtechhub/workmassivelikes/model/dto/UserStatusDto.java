package cn.com.edtechhub.workmassivelikes.model.dto;

import lombok.Data;

/**
 * 描述用户状态 DTO
 *
 * @author <a href="https://github.com/limou3434">limou3434</a>
 */
@Data
public class UserStatusDto {

    /**
     * 是否登录
     */
    Boolean isLogin;

    /**
     * Token 名称
     */
    String tokenName;

    /**
     * Token 有效时间
     */
    String tokenTimeout;

    /**
     * 登录用户 id
     */
    String userId;

}
