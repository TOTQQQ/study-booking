package com.studyroom.enums;

import lombok.Getter;

/**
 * 座位状态枚举 - 对齐数据库 seatStatus.status 字段
 * 数据库定义：1-可预约  2-已被预约
 */
@Getter
public enum SeatStatusEnum {

    AVAILABLE(1, "可预约"),

    RESERVED(2, "已被预约");

    private final Integer code;
    private final String desc;

    SeatStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
