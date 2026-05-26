package com.studyroom.service;

import com.studyroom.dto.CheckInDTO;
import com.studyroom.vo.ReservationVO;

/**
 * 签到服务接口
 * 负责人：严雯琪
 * 任务ID：2.3 签到接口开发（手动确认签到）
 */
public interface CheckInService {

    /**
     * 手动确认签到
     * @param userId 用户ID
     * @param checkInDTO 签到参数
     * @return 签到结果
     */
    ReservationVO confirmCheckIn(Long userId, CheckInDTO checkInDTO);

    /**
     * 扫码签到
     * @param userId 用户ID
     * @param seatId 座位ID
     * @return 签到结果
     */
    ReservationVO scanCheckIn(Long userId, Long seatId);

    /**
     * 获取签到倒计时（秒）
     * @param userId 用户ID
     * @param reservationId 预约ID
     * @return 剩余秒数
     */
    Long getCountdown(Long userId, Long reservationId);

    /**
     * 检查是否可以签到
     * @param userId 用户ID
     * @param reservationId 预约ID
     * @return 是否可以签到
     */
    Boolean canCheckIn(Long userId, Long reservationId);

    /**
     * 获取今日签到状态
     * @param userId 用户ID
     * @return 签到状态信息
     */
    ReservationVO getTodayCheckInStatus(Long userId);
}
