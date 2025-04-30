package cn.com.edtechhub.workmassivelikes.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户注册请求
 *
 * @author <a href="https://github.com/limou3434">limou3434</a>
 */
@Data
public class UserRegisterRequest implements Serializable {

    /**
     * 账户号(业务层需要决定某一种或多种登录方式, 因此这里不限死为非空)
     */
    private String account;

    /**
     * 用户密码(业务层强制刚刚注册的用户重新设置密码, 交给用户时默认密码为 123456, 并且加盐密码)
     */
    private String passwd;

    private String checkPasswd; // 需要再次确认密码

    /// 序列化字段 ///
    private static final long serialVersionUID = 1L;

}
