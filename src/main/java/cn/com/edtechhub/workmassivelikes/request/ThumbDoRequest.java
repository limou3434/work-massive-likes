package cn.com.edtechhub.workmassivelikes.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class ThumbDoRequest implements Serializable {

    /**
     * 需要被确认点赞的博客 ID
     */
    private Long blogId;

    /// 序列化字段 ///
    private static final long serialVersionUID = 1L;

}
