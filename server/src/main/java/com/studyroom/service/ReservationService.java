package com.studyroom.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.studyroom.dto.CancelDTO;
import com.studyroom.dto.ReserveDTO;
import com.studyroom.vo.ReservationVO;

import java.time.LocalDate;

/**
 * 预约服务接口
 * 负责人：严雯琪
 * 任务ID：2.2 预约接口开发（选择时间段、提交预约）
 */
public interface ReservationService {

    /**
     * 提交座位预约
     * @param userId 用户ID
     * @param reserveDTO 预约参数
     * @return 预约结果
     */
    ReservationVO reserve(Long userId, ReserveDTO reserveDTO);

    /**
     * 取消预约
     * @param userId 用户ID
     * @param cancelDTO 取消参数
     */
    void cancel(Long userId, CancelDTO cancelDTO);

    /**
     * 获取我的预约列表
     * @param userId 用户ID
     * @param status 预约状态
     * @param page 页码
     * @param size 每页大小
     * @return 预约列表
     */
    Page<ReservationVO> getMyReservations(Long userId, Integer status, Integer page, Integer size);

    /**
     * 获取预约详情
     * @param userId 用户ID
     * @param reservationId 预约ID
     * @return 预约详情
     */
    ReservationVO getReservationDetail(Long userId, Long reservationId);

    /**
     * 获取当前有效预约
     * @param userId 用户ID
     * @return 当前有效预约
     */
    ReservationVO getCurrentReservation(Long userId);

    /**
     * 获取历史预约记录
     * @param userId 用户ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param page 页码
     * @param size 每页大小
     * @return 历史记录
     */
    Page<ReservationVO> getHistory(Long userId, LocalDate startDate, LocalDate endDate, Integer page, Integer size);

    /**
     * 检查座位是否可预约
     * @param seatId 座位ID
     * @param date 预约日期
     * @param periodId 时间段ID
     * @return 是否可预约
     */
    Boolean checkAvailable(Long seatId, LocalDate date, Long periodId);
}
