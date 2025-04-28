package cn.com.edtechhub.workmassivelikes.contant;

/**
 * Redis 键名前缀常量
 */
public interface ThumbConstant {

    /**
     * 用户点赞记录键名前缀
     */
    String USER_THUMB_KEY_PREFIX = "thumb:"; // key(user_id): "field(blog_id)=value(thumb_id)"

    /**
     * 临时点赞记录键名前缀
     */
    String TEMP_THUMB_KEY_PREFIX = "thumb:temp:%s"; // key(time_slice): "field(user_id:blog_id)=value(is_thumb)"

}
