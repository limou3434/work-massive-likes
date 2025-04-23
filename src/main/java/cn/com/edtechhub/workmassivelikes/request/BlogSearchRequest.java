package cn.com.edtechhub.workmassivelikes.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true) // 自动生成 equals() 和 hashCode(), callSuper = true 使得 equals() 和 hashCode() 方法同时考虑 PageRequest 和 UserSearchRequest 的字段, 避免出现意料之外的情况
@Data
public class BlogSearchRequest extends PageRequest implements Serializable {

    /**
     * 本用户唯一标识(业务层需要考虑使用雪花算法用户标识的唯一性)
     */
    private Long id;

    /**
     * 角色标识
     */
    private Long userId;

    /**
     * 标题
     */
    private String title;

    /**
     * 封面
     */
    private String coverImg;

    /**
     * 内容
     */
    private String content;

    /**
     * 点赞数
     */
    private Integer thumbCount;

    /// 序列化字段 ///
    private static final long serialVersionUID = 1L;

}
