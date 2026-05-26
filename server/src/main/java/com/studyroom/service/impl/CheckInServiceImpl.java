package com.studyroom.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.studyroom.constant.RedisConstant;
import com.studyroom.dto.CheckInDTO;
import com.studyroom.entity.CheckIn;
import com.studyroom.entity.Reservation;
import com.studyroom.entity.Seat;
import com.studyroom.entity.TimePeriod;
import com.studyroom.enums.ReservationStatusEnum;
import com.studyroom.enums.SeatStatusEnum;
import com.studyroom.exception.BusinessException;
import com.studyroom.exception.ErrorCode;
import com.studyroom.mapper.CheckInMapper;
import com.studyroom.mapper.ReservationMapper;
import com.studyroom.mapper.SeatMapper;
import com.studyroom.mapper.TimePeriodMapper;
import com.studyroom.service.CheckInService;
import com.studyroom.utils.RedisUtil;
import com.studyroom.vo.ReservationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 签到服务实现类
 * 负责人：严雯琪
 * 任务ID：2.3 签到接口开发（手动确认签到）
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CheckInServiceImpl implements CheckInService {

    private final CheckInMapper checkInMapper;
    private final ReservationMapper reservationMapper;
    private final SeatMapper seatMapper;
    private final TimePeriodMapper timePeriodMapper;
    private final RedisUtil redisUtil;

    // 签到时间窗口（时间段开始前多少分钟可以签到）
    private static final int CHECKIN_WINDOW_MINUTES = 30;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReservationVO confirmCheckIn(Long userId, CheckInDTO checkInDTO) {
        Long reservationId = checkInDTO.getReservationId();

        // 1. 查询预约记录
        Reservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null) {
            throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND, "预约记录不存在");
        }

        // 2. 验证是否是用户自己的预约
        if (!reservation.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_YOUR_RESERVATION, "这不是您的预约");
        }

        // 3. 检查预约状态
        if (reservation.getStatus() != ReservationStatusEnum.PENDING.getCode()) {
            if (reservation.getStatus() == ReservationStatusEnum.CHECKED_IN.getCode()) {
                throw new BusinessException(ErrorCode.ALREADY_CHECKED_IN, "您已经签到过了");
            } else if (reservation.getStatus() == ReservationStatusEnum.CANCELLED.getCode() 
                    || reservation.getStatus() == ReservationStatusEnum.AUTO_CANCELLED.getCode()) {
                throw new BusinessException(ErrorCode.RESERVATION_CANCELLED, "该预约已取消");
            }
        }

        // 4. 检查签到时间是否有效
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = reservation.getCheckinDeadline();
        
        // 获取时间段信息
        TimePeriod period = timePeriodMapper.selectById(reservation.getPeriodId());
        LocalDateTime periodStart = LocalDateTime.of(reservation.getDate(), period.getStartTime());
        LocalDateTime checkinStart = periodStart.minusMinutes(CHECKIN_WINDOW_MINUTES);

        if (now.isBefore(checkinStart)) {
            throw new BusinessException(ErrorCode.TOO_EARLY_TO_CHECKIN, 
                "签到时间未到，请在时间段开始前" + CHECKIN_WINDOW_MINUTES + "分钟内签到");
        }

        if (now.isAfter(deadline)) {
            throw new BusinessException(ErrorCode.CHECKIN_TIMEOUT, "签到超时，预约已自动取消");
        }

        // 5. 创建签到记录
        CheckIn checkIn = new CheckIn();
        checkIn.setReservationId(reservationId);
        checkIn.setUserId(userId);
        checkIn.setSeatId(reservation.getSeatId());
        checkIn.setCheckinTime(now);
        checkIn.setCheckinType(1); // 1-手动签到
        checkIn.setCreateTime(now);
        checkInMapper.insert(checkIn);

        // 6. 更新预约状态为已签到
        LambdaUpdateWrapper<Reservation> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Reservation::getId, reservationId)
                .set(Reservation::getStatus, ReservationStatusEnum.CHECKED_IN.getCode())
                .set(Reservation::getCheckinTime, now)
                .set(Reservation::getUpdateTime, now);
        reservationMapper.update(null, updateWrapper);

        // 7. 更新座位状态为使用中
        LambdaUpdateWrapper<com.studyroom.entity.SeatStatus> seatStatusUpdate = new LambdaUpdateWrapper<>();
        seatStatusUpdate.eq(com.studyroom.entity.SeatStatus::getSeatId, reservation.getSeatId())
                .eq(com.studyroom.entity.SeatStatus::getDate, reservation.getDate())
                .eq(com.studyroom.entity.SeatStatus::getPeriodId, reservation.getPeriodId())
                .set(com.studyroom.entity.SeatStatus::getStatus, SeatStatusEnum.IN_USE.getCode())
                .set(com.studyroom.entity.SeatStatus::getUpdateTime, now);
        checkInMapper.updateSeatStatus(seatStatusUpdate);

        // 8. 删除签到倒计时缓存
        String reservationKey = RedisConstant.RESERVATION_TIMEOUT_KEY + reservation.getReservationNo();
        redisUtil.delete(reservationKey);

        // 9. 更新座位缓存
        String cacheKey = RedisConstant.SEAT_STATUS_KEY + reservation.getDate() + ":" + reservation.getPeriodId();
        redisUtil.hset(cacheKey, reservation.getSeatId().toString(), SeatStatusEnum.IN_USE.getCode().toString());

        log.info("签到成功，reservationId: {}, userId: {}, checkInTime: {}", reservationId, userId, now);

        // 10. 构建返回结果
        reservation.setStatus(ReservationStatusEnum.CHECKED_IN.getCode());
        reservation.setCheckinTime(now);
        Seat seat = seatMapper.selectById(reservation.getSeatId());
        return buildReservationVO(reservation, seat, period);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReservationVO scanCheckIn(Long userId, Long seatId) {
        // 1. 检查座位是否存在
        Seat seat = seatMapper.selectById(seatId);
        if (seat == null) {
            throw new BusinessException(ErrorCode.SEAT_NOT_FOUND, "座位不存在");
        }

        // 2. 查找用户今天对该座位的待签到预约
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<Reservation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Reservation::getUserId, userId)
                .eq(Reservation::getSeatId, seatId)
                .eq(Reservation::getDate, today)
                .eq(Reservation::getStatus, ReservationStatusEnum.PENDING.getCode());
        Reservation reservation = reservationMapper.selectOne(queryWrapper);

        if (reservation == null) {
            throw new BusinessException(ErrorCode.NO_PENDING_RESERVATION, "您没有该座位待签到的预约");
        }

        // 3. 执行签到
        CheckInDTO checkInDTO = new CheckInDTO();
        checkInDTO.setReservationId(reservation.getId());
        return confirmCheckIn(userId, checkInDTO);
    }

    @Override
    public Long getCountdown(Long userId, Long reservationId) {
        // 1. 查询预约记录
        Reservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null) {
            throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND, "预约记录不存在");
        }

        // 2. 验证是否是用户自己的预约
        if (!reservation.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_YOUR_RESERVATION, "这不是您的预约");
        }

        // 3. 检查预约状态
        if (reservation.getStatus() != ReservationStatusEnum.PENDING.getCode()) {
            return 0L; // 已签到或已取消，倒计时为0
        }

        // 4. 计算剩余秒数
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = reservation.getCheckinDeadline();

        if (now.isAfter(deadline)) {
            return 0L; // 已超时
        }

        return java.time.Duration.between(now, deadline).getSeconds();
    }

    @Override
    public Boolean canCheckIn(Long userId, Long reservationId) {
        try {
            Reservation reservation = reservationMapper.selectById(reservationId);
            if (reservation == null || !reservation.getUserId().equals(userId)) {
                return false;
            }

            if (reservation.getStatus() != ReservationStatusEnum.PENDING.getCode()) {
                return false;
            }

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime deadline = reservation.getCheckinDeadline();

            // 获取时间段信息
            TimePeriod period = timePeriodMapper.selectById(reservation.getPeriodId());
            LocalDateTime periodStart = LocalDateTime.of(reservation.getDate(), period.getStartTime());
            LocalDateTime checkinStart = periodStart.minusMinutes(CHECKIN_WINDOW_MINUTES);

            // 在签到时间窗口内且未超时
            return !now.isBefore(checkinStart) && !now.isAfter(deadline);
        } catch (Exception e) {
            log.error("检查签到状态失败", e);
            return false;
        }
    }

    @Override
    public ReservationVO getTodayCheckInStatus(Long userId) {
        LocalDate today = LocalDate.now();
        
        // 查找今天待签到的预约
        LambdaQueryWrapper<Reservation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Reservation::getUserId, userId)
                .eq(Reservation::getDate, today)
                .eq(Reservation::getStatus, ReservationStatusEnum.PENDING.getCode());
        Reservation reservation = reservationMapper.selectOne(queryWrapper);

        if (reservation == null) {
            // 查找今天已签到的预约
            LambdaQueryWrapper<Reservation> checkedInQuery = new LambdaQueryWrapper<>();
            checkedInQuery.eq(Reservation::getUserId, userId)
                    .eq(Reservation::getDate, today)
                    .eq(Reservation::getStatus, ReservationStatusEnum.CHECKED_IN.getCode());
            reservation = reservationMapper.selectOne(checkedInQuery);
        }

        if (reservation == null) {
            return null;
        }

        Seat seat = seatMapper.selectById(reservation.getSeatId());
        TimePeriod period = timePeriodMapper.selectById(reservation.getPeriodId());
        return buildReservationVO(reservation, seat, period);
    }

    /**
     * 构建预约VO
     */
    private ReservationVO buildReservationVO(Reservation reservation, Seat seat, TimePeriod period) {
        ReservationVO vo = new ReservationVO();
        BeanUtils.copyProperties(reservation, vo);
        if (seat != null) {
            vo.setSeatNo(seat.getSeatNo());
            vo.setSeatRow(seat.getRowNum());
            vo.setSeatCol(seat.getColNum());
        }
        if (period != null) {
            vo.setPeriodName(period.getPeriodName());
            vo.setStartTime(period.getStartTime());
            vo.setEndTime(period.getEndTime());
        }
        return vo;
    }
}
