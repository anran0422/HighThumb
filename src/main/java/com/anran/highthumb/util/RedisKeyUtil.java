package com.anran.highthumb.util;

import com.anran.highthumb.constant.ThumbConstant;

public class RedisKeyUtil {

    /**
     * 获取用户点赞记录 key
     */
    public static String getUserThumbKey(Long userId) {
        return ThumbConstant.USER_THUMB_KEY_PREFIX + userId;
    }

    /**
     * 获取临时点赞记录 key
     */
    public static String getTempThumbKey(String time) {
        return ThumbConstant.TEMP_THUMB_KEY_PREFIX.formatted(time);
    }
}
