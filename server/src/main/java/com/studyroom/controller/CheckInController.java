package com.studyroom.controller;

import com.studyroom.common.Result;
import com.studyroom.dto.CheckInDTO;
import com.studyroom.service.CheckInService;
import com.studyroom.vo.ReservationVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 签到控制器 - 座位签到、签到验证
 * 负责人：严雯琪
 * 任务ID：2.3 签到接口开发（手动确认签到）
 */
@Api(tags = "签到管理")
@RestController
@RequestMapping("/api/checkin")
@RequiredArgsConstructor
@Slf4j
public class CheckInController {

    private final CheckInService checkInService;

    /**
     * 手动签到
     * @param userId 用户ID（从JWT中获取）
     * @param checkInDTO 签到参数
     * @return 签到结果
     */
    @ApiOperation("手动签到")
    @PostMapping("/confirm")
    public Result<ReservationVO> confirmCheckIn(
            @RequestAttribute("userId") Long userId,
            @RequestBody @Validated CheckInDTO checkInDTO) {
        log.info("用户签到，userId: {}, reservationId: {}", userId, checkInDTO.getReservationId());
        ReservationVO result = checkInService.confirmCheckIn(userId, checkInDTO);
        return Result.success(result);
    }

    /**
     * 扫码签到（通过座位二维码）
     * @param userId 用户ID（从JWT中获取）
     * @param seatId 座位ID（从二维码获取）
     * @return 签到结果
     */
    @ApiOperation("扫码签到")
    @PostMapping("/scan")
    public Result<ReservationVO> scanCheckIn(
            @RequestAttribute("userId") Long userId,
            @RequestParam Long seatId) {
        log.info("用户扫码签到，userId: {}, seatId: {}", userId, seatId);
        ReservationVO result = checkInService.scanCheckIn(userId, seatId);
        return Result.success(result);
    }

    /**
     * 获取签到倒计时
     * @param userId 用户ID（从JWT中获取）
     * @param reservationId 预约ID
     * @return 剩余秒数
     */
    @ApiOperation("获取签到倒计时")
    @GetMapping("/countdown")
    public Result<Long> getCountdown(
            @RequestAttribute("userId") Long userId,
            @RequestParam Long reservationId) {
        log.info("获取签到倒计时，userId: {}, reservationId: {}", userId, reservationId);
        Long countdown = checkInService.getCountdown(userId, reservationId);
        return Result.success(countdown);
    }

    /**
     * 检查是否可以签到
     * @param userId 用户ID（从JWT中获取）
     * @param reservationId 预约ID
     * @return 是否可以签到
     */
    @ApiOperation("检查是否可以签到")
    @GetMapping("/check")
    public Result<Boolean> canCheckIn(
            @RequestAttribute("userId") Long userId,
            @RequestParam Long reservationId) {
        log.info("检查是否可以签到，userId: {}, reservationId: {}", userId, reservationId);
        Boolean canCheckIn = checkInService.canCheckIn(userId, reservationId);
        return Result.success(canCheckIn);
    }

    /**
     * 获取今日签到状态
     * @param userId 用户ID（从JWT中获取）
     * @return 签到状态信息
     */
    @ApiOperation("获取今日签到状态")
    @GetMapping("/today")
    public Result<ReservationVO> getTodayCheckInStatus(@RequestAttribute("userId") Long userId) {
        log.info("获取今日签到状态，userId: {}", userId);
        ReservationVO result = checkInService.getTodayCheckInStatus(userId);
        return Result.success(result);
    }
}
