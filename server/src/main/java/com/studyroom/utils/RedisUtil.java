package com.studyroom.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis工具类
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisUtil {

    private final StringRedisTemplate redisTemplate;

    /**
     * 设置缓存
     * @param key 键
     * @param value 值
     * @param timeout 超时时间
     * @param unit 时间单位
     */
    public void set(String key, String value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    /**
     * 获取缓存
     * @param key 键
     * @return 值
     */
    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除缓存
     * @param key 键
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    /**
     * 设置Hash缓存
     * @param key 键
     * @param hashKey Hash键
     * @param value 值
     */
    public void hset(String key, String hashKey, String value) {
        redisTemplate.opsForHash().put(key, hashKey, value);
    }

    /**
     * 获取Hash缓存
     * @param key 键
     * @param hashKey Hash键
     * @return 值
     */
    public String hget(String key, String hashKey) {
        Object value = redisTemplate.opsForHash().get(key, hashKey);
        return value != null ? value.toString() : null;
    }

    /**
     * 尝试获取分布式锁
     * @param key 锁键
     * @param waitTime 等待时间
     * @param unit 时间单位
     * @return 是否获取成功
     */
    public boolean tryLock(String key, long waitTime, TimeUnit unit) {
        try {
            Boolean success = redisTemplate.opsForValue()
                    .setIfAbsent(key, "1", waitTime, unit);
            return Boolean.TRUE.equals(success);
        } catch (Exception e) {
            log.error("获取锁失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 释放分布式锁
     * @param key 锁键
     */
    public void unlock(String key) {
        redisTemplate.delete(key);
    }
}
