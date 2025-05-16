package com.anran.highthumb.model.enums;


import lombok.Getter;

/**
 * 点赞原因
 */
@Getter
public enum ThumbTypeEnum {

    // 点赞
    INCR(1),

    // 取消点赞
    DECR(-1),

    // 不发生改变
    NON(0);

    private final int value;

    ThumbTypeEnum(int value) {
        this.value = value;
    }
}
