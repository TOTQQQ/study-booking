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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 预约控制器 - 对齐数据库表结构
 * 负责人：严雯琪
 * 任务ID：2.2 预约接口开发（选择时间段、提交预约）
 */
@Api(tags = "预约管理")
@RestController
@RequestMapping("/reservation")
@RequiredArgsConstructor
@Slf4j
public class ReservationController {

    private final ReservationService reservationService;

    @ApiOperation("提交座位预约")
    @PostMapping("/reserve")
    public Result<ReservationVO> reserve(
            @RequestAttribute("userId") Long userId,
            @RequestBody @Validated ReserveDTO reserveDTO) {
        log.info("用户预约座位，userId: {}, seatId: {}, date: {}, timePeriodId: {}",
            userId, reserveDTO.getSeatId(), reserveDTO.getDate(), reserveDTO.getTimePeriodId());
        ReservationVO result = reservationService.reserve(userId, reserveDTO);
        return Result.success(result);
    }

    @ApiOperation("取消预约")
    @PostMapping("/cancel")
    public Result<Void> cancel(
            @RequestAttribute("userId") Long userId,
            @RequestBody @Validated CancelDTO cancelDTO) {
        log.info("用户取消预约，userId: {}, reservationId: {}", userId, cancelDTO.getReservationId());
        reservationService.cancel(userId, cancelDTO);
        return Result.success();
    }

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

    @ApiOperation("获取预约详情")
    @GetMapping("/{reservationId}")
    public Result<ReservationVO> getReservationDetail(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long reservationId) {
        log.info("获取预约详情，userId: {}, reservationId: {}", userId, reservationId);
        ReservationVO result = reservationService.getReservationDetail(userId, reservationId);
        return Result.success(result);
    }

    @ApiOperation("获取当前有效预约")
    @GetMapping("/current")
    public Result<ReservationVO> getCurrentReservation(@RequestAttribute("userId") Long userId) {
        log.info("获取当前有效预约，userId: {}", userId);
        ReservationVO result = reservationService.getCurrentReservation(userId);
        return Result.success(result);
    }

    @ApiOperation("获取历史预约记录")
    @GetMapping("/history")
    public Result<Page<ReservationVO>> getHistory(
            @RequestAttribute("userId") Long userId,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        log.info("获取历史预约记录，userId: {}, startDate: {}, endDate: {}", userId, startDate, endDate);
        Page<ReservationVO> result = reservationService.getHistory(userId, startDate, endDate, page, size);
        return Result.success(result);
    }

    @ApiOperation("检查座位是否可预约")
    @GetMapping("/check")
    public Result<Boolean> checkAvailable(
            @RequestParam Integer seatId,
            @RequestParam String date,
            @RequestParam Integer timePeriodId) {
        log.info("检查座位是否可预约，seatId: {}, date: {}, timePeriodId: {}", seatId, date, timePeriodId);
        Boolean available = reservationService.checkAvailable(seatId, date, timePeriodId);
        return Result.success(available);
    }
}
