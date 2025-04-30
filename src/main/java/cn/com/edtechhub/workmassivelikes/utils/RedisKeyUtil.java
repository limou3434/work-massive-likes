package cn.com.edtechhub.workmassivelikes.utils;

import cn.com.edtechhub.workmassivelikes.contant.ThumbConstant;

/**
 * 获取键名前缀工具
 *
 * @author <a href="https://github.com/limou3434">limou3434</a>
 */
public class RedisKeyUtil {

    /**
     * 获取用户点赞记录 key 名字
     */
    public static String getUserThumbKey(String userId) {
        return ThumbConstant.USER_THUMB_KEY_PREFIX + userId;
    }

    /**
     * 获取临时点赞记录 key 名字
     */
    public static String getTempThumbKey(String time) {
        return ThumbConstant.TEMP_THUMB_KEY_PREFIX.formatted(time); // <=> return String.format(ThumbConstant.TEMP_THUMB_KEY_PREFIX, time);
    }

}
