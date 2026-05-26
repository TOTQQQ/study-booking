package com.studyroom.exception;

import lombok.Getter;

/**
 * 错误码枚举
 */
@Getter
public enum ErrorCode {

    // 系统错误
    SYSTEM_ERROR(500, "系统错误"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),

    // 认证相关
    WECHAT_LOGIN_FAILED(1001, "微信登录失败"),
    TOKEN_INVALID(1002, "令牌无效"),
    USER_NOT_FOUND(1003, "用户不存在"),

    // 座位相关
    SEAT_NOT_FOUND(2001, "座位不存在"),
    SEAT_NOT_AVAILABLE(2002, "座位不可用"),
    SEAT_BUSY(2003, "座位正在被预约"),

    // 时间段相关
    PERIOD_NOT_FOUND(3001, "时间段不存在"),

    // 预约相关
    INVALID_DATE(4001, "无效的预约日期"),
    ALREADY_RESERVED(4002, "您当天已有待签到的预约"),
    RESERVATION_NOT_FOUND(4003, "预约记录不存在"),
    NOT_YOUR_RESERVATION(4004, "这不是您的预约"),
    CANNOT_CANCEL(4005, "该预约无法取消"),

    // 签到相关
    ALREADY_CHECKED_IN(5001, "已经签到过了"),
    RESERVATION_CANCELLED(5002, "预约已取消"),
    TOO_EARLY_TO_CHECKIN(5003, "签到时间未到"),
    CHECKIN_TIMEOUT(5004, "签到超时"),
    NO_PENDING_RESERVATION(5005, "没有待签到的预约");

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
