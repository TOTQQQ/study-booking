package com.studyroom.cache;

import com.studyroom.constant.RedisConstant;
import com.studyroom.entity.SeatStatus;
import com.studyroom.enums.SeatStatusEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 座位缓存服务
 * 负责座位状态的Redis缓存读写
 *
 * @author 刘思琪
 * @date 2026-06-08
 */
@Service
public class SeatCacheService {

    private static final Logger log = LoggerFactory.getLogger(SeatCacheService.class);

    private final RedisTemplate<String, Object> redisTemplate;

    public SeatCacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 构建座位状态缓存Key
     * 格式：seat:status:2026-06-08:1
     */
    private String buildStatusKey(String date, Integer roomId) {
        return RedisConstant.SEAT_STATUS_KEY + date + ":" + roomId;
    }

    /**
     * 从缓存获取某天某自习室的所有座位状态
     * 
     * @param date   日期（yyyy-MM-dd）
     * @param roomId 自习室ID
     * @return Map<座位ID_时间段ID, 状态码>，缓存不存在返回null
     */
    public Map<String, Integer> getSeatStatusFromCache(String date, Integer roomId) {
        String key = buildStatusKey(date, roomId);
        
        // 获取整个Hash
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        
        if (entries == null || entries.isEmpty()) {
            log.debug("缓存未命中: key={}", key);
            return null;
        }
        
        // 转换格式：Object -> String -> Integer
        Map<String, Integer> result = new HashMap<>();
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            String field = entry.getKey().toString();
            Integer value = Integer.valueOf(entry.getValue().toString());
            result.put(field, value);
        }
        
        log.debug("缓存命中: key={}, 共{}条记录", key, result.size());
        return result;
    }

    /**
     * 获取单个座位在某个时间段的座位状态
     *
     * @param date         日期
     * @param roomId       自习室ID
     * @param seatId       座位ID
     * @param timePeriodId 时间段ID
     * @return 状态码，缓存不存在返回null
     */
    public Integer getSingleSeatStatus(String date, Integer roomId, Integer seatId, Integer timePeriodId) {
        String key = buildStatusKey(date, roomId);
        String field = seatId + "_" + timePeriodId;
        
        Object value = redisTemplate.opsForHash().get(key, field);
        if (value == null) {
            return null;
        }
        return Integer.valueOf(value.toString());
    }

    /**
     * 批量写入座位状态到缓存
     *
     * @param date      日期
     * @param roomId    自习室ID
     * @param statusMap 座位状态Map（key: seatId_timePeriodId, value: 状态码）
     */
    public void setSeatStatusToCache(String date, Integer roomId, Map<String, Integer> statusMap) {
        if (statusMap == null || statusMap.isEmpty()) {
            return;
        }
        
        String key = buildStatusKey(date, roomId);
        
        // 转换为Hash格式存储
        Map<String, Object> hashMap = new HashMap<>(statusMap);
        redisTemplate.opsForHash().putAll(key, hashMap);
        
        // 设置过期时间24小时
        redisTemplate.expire(key, RedisConstant.SEAT_STATUS_EXPIRE_TIME, TimeUnit.SECONDS);
        
        log.info("写入座位状态缓存: key={}, 共{}条记录", key, statusMap.size());
    }

    /**
     * 更新单个座位状态（预约/取消/签到时调用）
     *
     * @param date         日期
     * @param roomId       自习室ID
     * @param seatId       座位ID
     * @param timePeriodId 时间段ID
     * @param status       新状态码
     */
    public void updateSingleSeatStatus(String date, Integer roomId, Integer seatId, 
                                        Integer timePeriodId, Integer status) {
        String key = buildStatusKey(date, roomId);
        String field = seatId + "_" + timePeriodId;
        
        redisTemplate.opsForHash().put(key, field, status);
        
        // 刷新过期时间
        redisTemplate.expire(key, RedisConstant.SEAT_STATUS_EXPIRE_TIME, TimeUnit.SECONDS);
        
        log.debug("更新单个座位缓存: key={}, field={}, status={}", key, field, status);
    }

    /**
     * 删除某天某自习室的座位状态缓存
     * （当座位状态批量更新或数据不一致时调用）
     *
     * @param date   日期
     * @param roomId 自习室ID
     */
    public void deleteSeatStatusCache(String date, Integer roomId) {
        String key = buildStatusKey(date, roomId);
        Boolean deleted = redisTemplate.delete(key);
        log.info("删除座位状态缓存: key={}, result={}", key, deleted);
    }

    /**
     * 判断缓存是否存在
     *
     * @param date   日期
     * @param roomId 自习室ID
     * @return true=存在 false=不存在
     */
    public boolean isCached(String date, Integer roomId) {
        String key = buildStatusKey(date, roomId);
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }
}
