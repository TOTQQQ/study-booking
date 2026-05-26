package com.studyroom.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.studyroom.common.Result;
import com.studyroom.dto.CancelDTO;
import com.studyroom.dto.ReserveDTO;
import com.studyroom.service.ReservationService;
import com.studyroom.vo.ReservationVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 预约控制器 - 座位预约、取消预约
 * 负责人：严雯琪
 * 任务ID：2.2 预约接口开发（选择时间段、提交预约）
 */
@Api(tags = "预约管理")
@RestController
@RequestMapping("/api/reservation")
@RequiredArgsConstructor
@Slf4j
public class ReservationController {

    private final ReservationService reservationService;

    /**
     * 提交座位预约
     * @param userId 用户ID（从JWT中获取）
     * @param reserveDTO 预约参数
     * @return 预约结果
     */
    @ApiOperation("提交座位预约")
    @PostMapping("/reserve")
    public Result<ReservationVO> reserve(
            @RequestAttribute("userId") Long userId,
            @RequestBody @Validated ReserveDTO reserveDTO) {
        log.info("用户预约座位，userId: {}, seatId: {}, date: {}, periodId: {}", 
            userId, reserveDTO.getSeatId(), reserveDTO.getDate(), reserveDTO.getPeriodId());
        ReservationVO result = reservationService.reserve(userId, reserveDTO);
        return Result.success(result);
    }

    /**
     * 取消预约
     * @param userId 用户ID（从JWT中获取）
     * @param cancelDTO 取消参数
     * @return 操作结果
     */
    @ApiOperation("取消预约")
    @PostMapping("/cancel")
    public Result<Void> cancel(
            @RequestAttribute("userId") Long userId,
            @RequestBody @Validated CancelDTO cancelDTO) {
        log.info("用户取消预约，userId: {}, reservationId: {}", userId, cancelDTO.getReservationId());
        reservationService.cancel(userId, cancelDTO);
        return Result.success();
    }

    /**
     * 获取我的预约列表
     * @param userId 用户ID（从JWT中获取）
     * @param status 预约状态（可选，1-待签到，2-已签到，3-已取消，4-自动取消）
     * @param page 页码
     * @param size 每页大小
     * @return 预约列表
     */
    @ApiOperation("获取我的预约列表")
    @GetMapping("/my")
    public Result<Page<ReservationVO>> getMyReservations(
            @RequestAttribute("userId") Long userId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        log.info("获取我的预约列表，userId: {}, status: {}", userId, status);
        Page<ReservationVO> result = reservationService.getMyReservations(userId, status, page, size);
        return Result.success(result);
    }

    /**
     * 获取预约详情
     * @param userId 用户ID（从JWT中获取）
     * @param reservationId 预约ID
     * @return 预约详情
     */
    @ApiOperation("获取预约详情")
    @GetMapping("/{reservationId}")
    public Result<ReservationVO> getReservationDetail(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long reservationId) {
        log.info("获取预约详情，userId: {}, reservationId: {}", userId, reservationId);
        ReservationVO result = reservationService.getReservationDetail(userId, reservationId);
        return Result.success(result);
    }

    /**
     * 获取当前有效预约（用于首页展示）
     * @param userId 用户ID（从JWT中获取）
     * @return 当前有效预约
     */
    @ApiOperation("获取当前有效预约")
    @GetMapping("/current")
    public Result<ReservationVO> getCurrentReservation(@RequestAttribute("userId") Long userId) {
        log.info("获取当前有效预约，userId: {}", userId);
        ReservationVO result = reservationService.getCurrentReservation(userId);
        return Result.success(result);
    }

    /**
     * 获取历史预约记录
     * @param userId 用户ID（从JWT中获取）
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param page 页码
     * @param size 每页大小
     * @return 历史记录
     */
    @ApiOperation("获取历史预约记录")
    @GetMapping("/history")
    public Result<Page<ReservationVO>> getHistory(
            @RequestAttribute("userId") Long userId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        log.info("获取历史预约记录，userId: {}, startDate: {}, endDate: {}", userId, startDate, endDate);
        Page<ReservationVO> result = reservationService.getHistory(userId, startDate, endDate, page, size);
        return Result.success(result);
    }

    /**
     * 检查座位是否可预约
     * @param seatId 座位ID
     * @param date 预约日期
     * @param periodId 时间段ID
     * @return 是否可预约
     */
    @ApiOperation("检查座位是否可预约")
    @GetMapping("/check")
    public Result<Boolean> checkAvailable(
            @RequestParam Long seatId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @RequestParam Long periodId) {
        log.info("检查座位是否可预约，seatId: {}, date: {}, periodId: {}", seatId, date, periodId);
        Boolean available = reservationService.checkAvailable(seatId, date, periodId);
        return Result.success(available);
    }
}
