package cn.com.edtechhub.workmassivelikes.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户删除请求
 *
 * @author <a href="https://github.com/limou3434">limou3434</a>
 */
@Data
public class UserDeleteRequest implements Serializable {

    /**
     * 本用户唯一标识(业务层需要考虑使用雪花算法用户标识的唯一性)
     */
    private Long id;

    /// 序列化字段 ///
    private static final long serialVersionUID = 1L;

}
