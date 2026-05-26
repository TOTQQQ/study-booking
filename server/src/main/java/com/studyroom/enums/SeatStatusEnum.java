package com.studyroom.enums;

import lombok.Getter;

/**
 * 座位状态枚举
 */
@Getter
public enum SeatStatusEnum {

    /**
     * 可预约
     */
    AVAILABLE(0, "可预约"),

    /**
     * 已预约
     */
    RESERVED(1, "已预约"),

    /**
     * 使用中
     */
    IN_USE(2, "使用中"),

    /**
     * 不可用
     */
    UNAVAILABLE(3, "不可用");

    private final Integer code;
    private final String desc;

    SeatStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
