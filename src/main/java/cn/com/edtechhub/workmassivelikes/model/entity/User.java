package cn.com.edtechhub.workmassivelikes.model.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 用户实体
 *
 * @author <a href="https://github.com/limou3434">limou3434</a>
 */
@TableName(value ="user")
@Data
public class User implements Serializable {
    /**
     * 本用户唯一标识(业务层需要考虑使用雪花算法用户标识的唯一性)
     */
    @TableId(type = IdType.ASSIGN_ID) // 手动添加雪花算法
    private Long id;

    /**
     * 账户号(业务层需要决定某一种或多种登录方式, 因此这里不限死为非空)
     */
    private String account;

    /**
     * 微信号
     */
    private String wxUnion;

    /**
     * 公众号
     */
    private String mpOpen;

    /**
     * 邮箱号
     */
    private String email;

    /**
     * 电话号
     */
    private String phone;

    /**
     * 身份证
     */
    private String ident;

    /**
     * 用户密码(业务层强制刚刚注册的用户重新设置密码, 交给用户时默认密码为 123456, 并且加盐密码)
     */
    private String passwd;

    /**
     * 用户头像(业务层需要考虑默认头像使用 cos 对象存储)
     */
    private String avatar;

    /**
     * 用户标签(业务层需要 json 数组格式存储用户标签数组)
     */
    private String tags;

    /**
     * 用户昵称
     */
    private String nick;

    /**
     * 用户名字
     */
    private String name;

    /**
     * 用户简介
     */
    private String profile;

    /**
     * 用户生日
     */
    private String birthday;

    /**
     * 用户国家
     */
    private String country;

    /**
     * 用户地址
     */
    private String address;

    /**
     * 用户角色(业务层需知 -1 为封号, 0 为用户, 1 为管理, ...)
     */
    private Integer role;

    /**
     * 用户等级(业务层需知 0 为 level0, 1 为 level1, 2 为 level2, 3 为 level3, ...)
     */
    private Integer level;

    /**
     * 用户性别(业务层需知 0 为未知, 1 为男性, 2 为女性)
     */
    private Integer gender;

    /**
     * 是否删除(0 为未删除, 1 为已删除)
     */
    @TableLogic
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
        User other = (User) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getAccount() == null ? other.getAccount() == null : this.getAccount().equals(other.getAccount()))
            && (this.getWxUnion() == null ? other.getWxUnion() == null : this.getWxUnion().equals(other.getWxUnion()))
            && (this.getMpOpen() == null ? other.getMpOpen() == null : this.getMpOpen().equals(other.getMpOpen()))
            && (this.getEmail() == null ? other.getEmail() == null : this.getEmail().equals(other.getEmail()))
            && (this.getPhone() == null ? other.getPhone() == null : this.getPhone().equals(other.getPhone()))
            && (this.getIdent() == null ? other.getIdent() == null : this.getIdent().equals(other.getIdent()))
            && (this.getPasswd() == null ? other.getPasswd() == null : this.getPasswd().equals(other.getPasswd()))
            && (this.getAvatar() == null ? other.getAvatar() == null : this.getAvatar().equals(other.getAvatar()))
            && (this.getTags() == null ? other.getTags() == null : this.getTags().equals(other.getTags()))
            && (this.getNick() == null ? other.getNick() == null : this.getNick().equals(other.getNick()))
            && (this.getName() == null ? other.getName() == null : this.getName().equals(other.getName()))
            && (this.getProfile() == null ? other.getProfile() == null : this.getProfile().equals(other.getProfile()))
            && (this.getBirthday() == null ? other.getBirthday() == null : this.getBirthday().equals(other.getBirthday()))
            && (this.getCountry() == null ? other.getCountry() == null : this.getCountry().equals(other.getCountry()))
            && (this.getAddress() == null ? other.getAddress() == null : this.getAddress().equals(other.getAddress()))
            && (this.getRole() == null ? other.getRole() == null : this.getRole().equals(other.getRole()))
            && (this.getLevel() == null ? other.getLevel() == null : this.getLevel().equals(other.getLevel()))
            && (this.getGender() == null ? other.getGender() == null : this.getGender().equals(other.getGender()))
            && (this.getDeleted() == null ? other.getDeleted() == null : this.getDeleted().equals(other.getDeleted()))
            && (this.getCreateTime() == null ? other.getCreateTime() == null : this.getCreateTime().equals(other.getCreateTime()))
            && (this.getUpdateTime() == null ? other.getUpdateTime() == null : this.getUpdateTime().equals(other.getUpdateTime()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getAccount() == null) ? 0 : getAccount().hashCode());
        result = prime * result + ((getWxUnion() == null) ? 0 : getWxUnion().hashCode());
        result = prime * result + ((getMpOpen() == null) ? 0 : getMpOpen().hashCode());
        result = prime * result + ((getEmail() == null) ? 0 : getEmail().hashCode());
        result = prime * result + ((getPhone() == null) ? 0 : getPhone().hashCode());
        result = prime * result + ((getIdent() == null) ? 0 : getIdent().hashCode());
        result = prime * result + ((getPasswd() == null) ? 0 : getPasswd().hashCode());
        result = prime * result + ((getAvatar() == null) ? 0 : getAvatar().hashCode());
        result = prime * result + ((getTags() == null) ? 0 : getTags().hashCode());
        result = prime * result + ((getNick() == null) ? 0 : getNick().hashCode());
        result = prime * result + ((getName() == null) ? 0 : getName().hashCode());
        result = prime * result + ((getProfile() == null) ? 0 : getProfile().hashCode());
        result = prime * result + ((getBirthday() == null) ? 0 : getBirthday().hashCode());
        result = prime * result + ((getCountry() == null) ? 0 : getCountry().hashCode());
        result = prime * result + ((getAddress() == null) ? 0 : getAddress().hashCode());
        result = prime * result + ((getRole() == null) ? 0 : getRole().hashCode());
        result = prime * result + ((getLevel() == null) ? 0 : getLevel().hashCode());
        result = prime * result + ((getGender() == null) ? 0 : getGender().hashCode());
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
        sb.append(", account=").append(account);
        sb.append(", wxUnion=").append(wxUnion);
        sb.append(", mpOpen=").append(mpOpen);
        sb.append(", email=").append(email);
        sb.append(", phone=").append(phone);
        sb.append(", ident=").append(ident);
        sb.append(", passwd=").append(passwd);
        sb.append(", avatar=").append(avatar);
        sb.append(", tags=").append(tags);
        sb.append(", nick=").append(nick);
        sb.append(", name=").append(name);
        sb.append(", profile=").append(profile);
        sb.append(", birthday=").append(birthday);
        sb.append(", country=").append(country);
        sb.append(", address=").append(address);
        sb.append(", role=").append(role);
        sb.append(", level=").append(level);
        sb.append(", gender=").append(gender);
        sb.append(", deleted=").append(deleted);
        sb.append(", createTime=").append(createTime);
        sb.append(", updateTime=").append(updateTime);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}
