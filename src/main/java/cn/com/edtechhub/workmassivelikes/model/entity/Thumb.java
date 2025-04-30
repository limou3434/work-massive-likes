package cn.com.edtechhub.workmassivelikes.model.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 点赞实体
 *
 * @author <a href="https://github.com/limou3434">limou3434</a>
 */
@TableName(value ="thumb")
@Data
public class Thumb implements Serializable {
    /**
     * 本点赞唯一标识(业务层需要考虑使用雪花算法用户标识的唯一性)
     */
    @TableId(type = IdType.ASSIGN_ID) // 手动添加雪花算法
    private Long id;

    /**
     * 用户标识
     */
    private Long userId;

    /**
     * 文章标识
     */
    private Long blogId;

    /**
     * 是否删除(0 为未删除, 1 为已删除)
     */
    // @TableLogic // 最好不要开启点赞记录的逻辑删除, 点赞其实称不上重要的数据
    private Integer deleted;

    /**
     * 创建时间(受时区影响)
     */
    private Date createTime;

    /**
     * 更新时间(受时区影响)
     */
    private Date updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        Thumb other = (Thumb) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getUserId() == null ? other.getUserId() == null : this.getUserId().equals(other.getUserId()))
            && (this.getBlogId() == null ? other.getBlogId() == null : this.getBlogId().equals(other.getBlogId()))
            && (this.getDeleted() == null ? other.getDeleted() == null : this.getDeleted().equals(other.getDeleted()))
            && (this.getCreateTime() == null ? other.getCreateTime() == null : this.getCreateTime().equals(other.getCreateTime()))
            && (this.getUpdateTime() == null ? other.getUpdateTime() == null : this.getUpdateTime().equals(other.getUpdateTime()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getUserId() == null) ? 0 : getUserId().hashCode());
        result = prime * result + ((getBlogId() == null) ? 0 : getBlogId().hashCode());
        result = prime * result + ((getDeleted() == null) ? 0 : getDeleted().hashCode());
        result = prime * result + ((getCreateTime() == null) ? 0 : getCreateTime().hashCode());
        result = prime * result + ((getUpdateTime() == null) ? 0 : getUpdateTime().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", userId=").append(userId);
        sb.append(", blogId=").append(blogId);
        sb.append(", deleted=").append(deleted);
        sb.append(", createTime=").append(createTime);
        sb.append(", updateTime=").append(updateTime);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}
