package com.studyroom.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.studyroom.constant.RedisConstant;
import com.studyroom.dto.CheckInDTO;
import com.studyroom.entity.Reservation;
import com.studyroom.entity.Seat;
import com.studyroom.entity.SeatStatus;
import com.studyroom.entity.StudyRoom;
import com.studyroom.entity.TimePeriod;
import com.studyroom.enums.ReservationStatusEnum;
import com.studyroom.enums.SeatStatusEnum;
import com.studyroom.exception.BusinessException;
import com.studyroom.exception.ErrorCode;
import com.studyroom.mapper.ReservationMapper;
import com.studyroom.mapper.SeatMapper;
import com.studyroom.mapper.SeatStatusMapper;
import com.studyroom.mapper.StudyRoomMapper;
import com.studyroom.mapper.TimePeriodMapper;
import com.studyroom.service.CheckInService;
import com.studyroom.utils.RedisUtil;
import com.studyroom.vo.ReservationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * 签到服务实现类 - 对齐数据库表结构
 * 签到信息直接记录在 reservation 表的 signTime 字段中
 * 负责人：严雯琪
 * 任务ID：2.3 签到接口开发（手动确认签到）
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CheckInServiceImpl implements CheckInService {

    private final ReservationMapper reservationMapper;
    private final SeatMapper seatMapper;
    private final SeatStatusMapper seatStatusMapper;
    private final TimePeriodMapper timePeriodMapper;
    private final StudyRoomMapper studyRoomMapper;
    private final RedisUtil redisUtil;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    // 签到时间窗口：时间段开始前多少分钟可以签到
    private static final int CHECKIN_WINDOW_MINUTES = 30;

    // 签到超时：时间段开始后多少分钟
    private static final int CHECKIN_TIMEOUT_MINUTES = 15;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReservationVO confirmCheckIn(Long userId, CheckInDTO checkInDTO) {
        Integer reservationId = checkInDTO.getReservationId();

        // 1. 查询预约记录
        Reservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null) {
            throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND, "预约记录不存在");
        }

        // 2. 验证是否是用户自己的预约
        if (!reservation.getUserId().equals(userId.intValue())) {
            throw new BusinessException(ErrorCode.NOT_YOUR_RESERVATION, "这不是您的预约");
        }

        // 3. 检查预约状态
        if (reservation.getStatus() != ReservationStatusEnum.PENDING.getCode()) {
            if (reservation.getStatus() == ReservationStatusEnum.CHECKED_IN.getCode()) {
                throw new BusinessException(ErrorCode.ALREADY_CHECKED_IN, "您已经签到过了");
            } else {
                throw new BusinessException(ErrorCode.RESERVATION_CANCELLED, "该预约已取消");
            }
        }

        // 4. 检查签到时间是否有效
        TimePeriod period = timePeriodMapper.selectById(reservation.getTimePeriodId());
        if (period == null) {
            throw new BusinessException(ErrorCode.PERIOD_NOT_FOUND, "时间段不存在");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDate reserveDate = LocalDate.parse(reservation.getDate(), DATE_FMT);
        LocalTime periodStart = LocalTime.parse(period.getStartTime(), TIME_FMT);

        LocalDateTime checkinStart = LocalDateTime.of(reserveDate, periodStart).minusMinutes(CHECKIN_WINDOW_MINUTES);
        LocalDateTime checkinDeadline = LocalDateTime.of(reserveDate, periodStart).plusMinutes(CHECKIN_TIMEOUT_MINUTES);

        if (now.isBefore(checkinStart)) {
            throw new BusinessException(ErrorCode.TOO_EARLY_TO_CHECKIN,
                    "签到时间未到，请在时间段开始前" + CHECKIN_WINDOW_MINUTES + "分钟内签到");
        }

        if (now.isAfter(checkinDeadline)) {
            // 超时自动取消
            long currentTime = System.currentTimeMillis();
            LambdaUpdateWrapper<Reservation> autoCancelWrapper = new LambdaUpdateWrapper<>();
            autoCancelWrapper.eq(Reservation::getId, reservationId)
                    .set(Reservation::getStatus, ReservationStatusEnum.AUTO_CANCELLED.getCode())
                    .set(Reservation::getCancelTime, currentTime)
                    .set(Reservation::getUpdateTime, currentTime);
            reservationMapper.update(null, autoCancelWrapper);

            // 恢复座位状态
            LambdaUpdateWrapper<SeatStatus> seatStatusUpdate = new LambdaUpdateWrapper<>();
            seatStatusUpdate.eq(SeatStatus::getSeatId, reservation.getSeatId())
                    .eq(SeatStatus::getDate, reservation.getDate())
                    .eq(SeatStatus::getTimePeriodId, reservation.getTimePeriodId())
                    .set(SeatStatus::getStatus, SeatStatusEnum.AVAILABLE.getCode())
                    .set(SeatStatus::getUpdateTime, currentTime);
            seatStatusMapper.update(null, seatStatusUpdate);

            throw new BusinessException(ErrorCode.CHECKIN_TIMEOUT, "签到超时，预约已自动取消");
        }

        // 5. 签到：更新 reservation 表的 signTime 和 status
        long signTimestamp = System.currentTimeMillis();

        LambdaUpdateWrapper<Reservation> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Reservation::getId, reservationId)
                .set(Reservation::getStatus, ReservationStatusEnum.CHECKED_IN.getCode())
                .set(Reservation::getSignTime, signTimestamp)
                .set(Reservation::getUpdateTime, signTimestamp);
        reservationMapper.update(null, updateWrapper);

        // 6. 更新座位状态为已预约（保持2-已被预约，因为座位仍在使用中）
        // 注意：数据库 seatStatus.status 只有 1-可预约 2-已被预约，没有"使用中"状态

        // 7. 更新座位缓存
        String cacheKey = RedisConstant.SEAT_STATUS_KEY + reservation.getDate() + ":" + reservation.getTimePeriodId();
        redisUtil.hset(cacheKey, reservation.getSeatId().toString(), SeatStatusEnum.RESERVED.getCode().toString());

        log.info("签到成功，reservationId: {}, userId: {}, signTime: {}", reservationId, userId, signTimestamp);

        // 8. 构建返回结果
        reservation.setStatus(ReservationStatusEnum.CHECKED_IN.getCode());
        reservation.setSignTime(signTimestamp);
        reservation.setUpdateTime(signTimestamp);

        Seat seat = seatMapper.selectById(reservation.getSeatId());
        return buildReservationVO(reservation, seat, period);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReservationVO scanCheckIn(Long userId, Long seatId) {
        // 1. 检查座位是否存在
        Seat seat = seatMapper.selectById(seatId.intValue());
        if (seat == null) {
            throw new BusinessException(ErrorCode.SEAT_NOT_FOUND, "座位不存在");
        }

        // 2. 查找用户今天对该座位的待签到预约
        String today = LocalDate.now().format(DATE_FMT);
        LambdaQueryWrapper<Reservation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Reservation::getUserId, userId.intValue())
                .eq(Reservation::getSeatId, seatId.intValue())
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
        Reservation reservation = reservationMapper.selectById(reservationId.intValue());
        if (reservation == null) {
            throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND, "预约记录不存在");
        }
        if (!reservation.getUserId().equals(userId.intValue())) {
            throw new BusinessException(ErrorCode.NOT_YOUR_RESERVATION, "这不是您的预约");
        }
        if (reservation.getStatus() != ReservationStatusEnum.PENDING.getCode()) {
            return 0L;
        }

        // 计算签到截止时间
        TimePeriod period = timePeriodMapper.selectById(reservation.getTimePeriodId());
        if (period == null) {
            return 0L;
        }

        LocalDate reserveDate = LocalDate.parse(reservation.getDate(), DATE_FMT);
        LocalTime periodStart = LocalTime.parse(period.getStartTime(), TIME_FMT);
        LocalDateTime deadline = LocalDateTime.of(reserveDate, periodStart).plusMinutes(CHECKIN_TIMEOUT_MINUTES);

        long remaining = java.time.Duration.between(LocalDateTime.now(), deadline).getSeconds();
        return Math.max(0, remaining);
    }

    @Override
    public Boolean canCheckIn(Long userId, Long reservationId) {
        try {
            Reservation reservation = reservationMapper.selectById(reservationId.intValue());
            if (reservation == null || !reservation.getUserId().equals(userId.intValue())) {
                return false;
            }
            if (reservation.getStatus() != ReservationStatusEnum.PENDING.getCode()) {
                return false;
            }

            TimePeriod period = timePeriodMapper.selectById(reservation.getTimePeriodId());
            if (period == null) {
                return false;
            }

            LocalDateTime now = LocalDateTime.now();
            LocalDate reserveDate = LocalDate.parse(reservation.getDate(), DATE_FMT);
            LocalTime periodStart = LocalTime.parse(period.getStartTime(), TIME_FMT);

            LocalDateTime checkinStart = LocalDateTime.of(reserveDate, periodStart).minusMinutes(CHECKIN_WINDOW_MINUTES);
            LocalDateTime checkinDeadline = LocalDateTime.of(reserveDate, periodStart).plusMinutes(CHECKIN_TIMEOUT_MINUTES);

            return !now.isBefore(checkinStart) && !now.isAfter(checkinDeadline);
        } catch (Exception e) {
            log.error("检查签到状态失败", e);
            return false;
        }
    }

    @Override
    public ReservationVO getTodayCheckInStatus(Long userId) {
        String today = LocalDate.now().format(DATE_FMT);

        // 查找今天待签到的预约
        LambdaQueryWrapper<Reservation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Reservation::getUserId, userId.intValue())
                .eq(Reservation::getDate, today)
                .eq(Reservation::getStatus, ReservationStatusEnum.PENDING.getCode());
        Reservation reservation = reservationMapper.selectOne(queryWrapper);

        if (reservation == null) {
            // 查找今天已签到的预约
            LambdaQueryWrapper<Reservation> checkedInQuery = new LambdaQueryWrapper<>();
            checkedInQuery.eq(Reservation::getUserId, userId.intValue())
                    .eq(Reservation::getDate, today)
                    .eq(Reservation::getStatus, ReservationStatusEnum.CHECKED_IN.getCode());
            reservation = reservationMapper.selectOne(checkedInQuery);
        }

        if (reservation == null) {
            return null;
        }

        Seat seat = seatMapper.selectById(reservation.getSeatId());
        TimePeriod period = timePeriodMapper.selectById(reservation.getTimePeriodId());
        return buildReservationVO(reservation, seat, period);
    }

    /**
     * 构建预约VO
     */
    private ReservationVO buildReservationVO(Reservation reservation, Seat seat, TimePeriod period) {
        ReservationVO vo = new ReservationVO();
        vo.setId(reservation.getId());
        vo.setReserveNo(reservation.getReserveNo());
        vo.setUserId(reservation.getUserId());
        vo.setSeatId(reservation.getSeatId());
        vo.setRoomId(reservation.getRoomId());
        vo.setTimePeriodId(reservation.getTimePeriodId());
        vo.setDate(reservation.getDate());
        vo.setStatus(reservation.getStatus());
        vo.setSignTime(reservation.getSignTime());
        vo.setCancelTime(reservation.getCancelTime());
        vo.setCreateTime(reservation.getCreateTime());
        vo.setUpdateTime(reservation.getUpdateTime());

        if (seat != null) {
            vo.setSeatCode(seat.getSeatCode());
            vo.setSeatX(seat.getX());
            vo.setSeatY(seat.getY());
        }
        if (period != null) {
            vo.setStartTime(period.getStartTime());
            vo.setEndTime(period.getEndTime());
        }

        // 查询自习室名称
        if (reservation.getRoomId() != null) {
            StudyRoom room = studyRoomMapper.selectById(reservation.getRoomId());
            if (room != null) {
                vo.setRoomName(room.getRoomName());
            }
        }

        return vo;
    }
}
