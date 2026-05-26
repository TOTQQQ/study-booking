package com.studyroom.constant;

/**
 * Redis Key常量
 */
public class RedisConstant {

    /**
     * 用户信息缓存前缀
     */
    public static final String USER_INFO_KEY = "user:info:";

    /**
     * 用户Token缓存前缀
     */
    public static final String USER_TOKEN_KEY = "user:token:";

    /**
     * 用户刷新Token缓存前缀
     */
    public static final String USER_REFRESH_TOKEN_KEY = "user:refresh_token:";

    /**
     * 座位状态缓存前缀
     */
    public static final String SEAT_STATUS_KEY = "seat:status:";

    /**
     * 座位分布式锁前缀
     */
    public static final String SEAT_LOCK_KEY = "seat:lock:";

    /**
     * 预约超时缓存前缀
     */
    public static final String RESERVATION_TIMEOUT_KEY = "reservation:timeout:";
}
