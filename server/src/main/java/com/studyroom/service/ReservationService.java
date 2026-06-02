package com.studyroom.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.studyroom.dto.CancelDTO;
import com.studyroom.dto.ReserveDTO;
import com.studyroom.vo.ReservationVO;

/**
 * 预约服务接口 - 对齐数据库表结构
 * 负责人：严雯琪
 * 任务ID：2.2 预约接口开发（选择时间段、提交预约）
 */
public interface ReservationService {

    /**
     * 提交座位预约
     */
    ReservationVO reserve(Long userId, ReserveDTO reserveDTO);

    /**
     * 取消预约
     */
    void cancel(Long userId, CancelDTO cancelDTO);

    /**
     * 获取我的预约列表
     */
    Page<ReservationVO> getMyReservations(Long userId, Integer status, Integer page, Integer size);

    /**
     * 获取预约详情
     */
    ReservationVO getReservationDetail(Long userId, Long reservationId);

    /**
     * 获取当前有效预约
     */
    ReservationVO getCurrentReservation(Long userId);

    /**
     * 获取历史预约记录
     */
    Page<ReservationVO> getHistory(Long userId, String startDate, String endDate, Integer page, Integer size);

    /**
     * 检查座位是否可预约
     */
    Boolean checkAvailable(Integer seatId, String date, Integer timePeriodId);
}
