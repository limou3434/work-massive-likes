package cn.com.edtechhub.workmassivelikes.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class ThumbUnDoRequest implements Serializable {

    /**
     * 需要被取消点赞的博客 ID
     */
    Long blogId;

    /// 序列化字段 ///
    private static final long serialVersionUID = 1L;

}
