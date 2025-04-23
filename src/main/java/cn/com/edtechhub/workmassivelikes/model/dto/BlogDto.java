package cn.com.edtechhub.workmassivelikes.model.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class BlogDto implements Serializable {

    /**
     * 本博客唯一标识(业务层需要考虑使用雪花算法用户标识的唯一性)
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

    /**
     * 创建时间(受时区影响)
     */
    private Date createTime;

    /**
     * 更新时间(受时区影响)
     */
    private Date updateTime;

    /**
     * 当前用户是否已点赞
     */
    private Boolean hasThumb;

}