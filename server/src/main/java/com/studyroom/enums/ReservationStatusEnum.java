package com.studyroom.enums;

import lombok.Getter;

/**
 * 预约状态枚举
 */
@Getter
public enum ReservationStatusEnum {

    /**
     * 待签到
     */
    PENDING(1, "待签到"),

    /**
     * 已签到
     */
    CHECKED_IN(2, "已签到"),

    /**
     * 已取消
     */
    CANCELLED(3, "已取消"),

    /**
     * 自动取消
     */
    AUTO_CANCELLED(4, "自动取消");

    private final Integer code;
    private final String desc;

    ReservationStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
