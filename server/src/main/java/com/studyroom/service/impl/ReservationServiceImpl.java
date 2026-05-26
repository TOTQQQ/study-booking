package com.studyroom.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.studyroom.constant.RedisConstant;
import com.studyroom.dto.CancelDTO;
import com.studyroom.dto.ReserveDTO;
import com.studyroom.entity.Reservation;
import com.studyroom.entity.Seat;
import com.studyroom.entity.SeatStatus;
import com.studyroom.entity.TimePeriod;
import com.studyroom.enums.ReservationStatusEnum;
import com.studyroom.enums.SeatStatusEnum;
import com.studyroom.exception.BusinessException;
import com.studyroom.exception.ErrorCode;
import com.studyroom.mapper.ReservationMapper;
import com.studyroom.mapper.SeatMapper;
import com.studyroom.mapper.SeatStatusMapper;
import com.studyroom.mapper.TimePeriodMapper;
import com.studyroom.service.ReservationService;
import com.studyroom.utils.IdGeneratorUtil;
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
import java.util.concurrent.TimeUnit;

/**
 * 预约服务实现类
 * 负责人：严雯琪
 * 任务ID：2.2 预约接口开发（选择时间段、提交预约）
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationServiceImpl implements ReservationService {

    private final ReservationMapper reservationMapper;
    private final SeatMapper seatMapper;
    private final SeatStatusMapper seatStatusMapper;
    private final TimePeriodMapper timePeriodMapper;
    private final RedisUtil redisUtil;
    private final IdGeneratorUtil idGeneratorUtil;

    // 签到超时时间（分钟）
    private static final int CHECKIN_TIMEOUT_MINUTES = 15;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReservationVO reserve(Long userId, ReserveDTO reserveDTO) {
        Long seatId = reserveDTO.getSeatId();
        LocalDate date = reserveDTO.getDate();
        Long periodId = reserveDTO.getPeriodId();

        // 1. 检查座位是否存在
        Seat seat = seatMapper.selectById(seatId);
        if (seat == null) {
            throw new BusinessException(ErrorCode.SEAT_NOT_FOUND, "座位不存在");
        }

        // 2. 检查时间段是否存在
        TimePeriod period = timePeriodMapper.selectById(periodId);
        if (period == null) {
            throw new BusinessException(ErrorCode.PERIOD_NOT_FOUND, "时间段不存在");
        }

        // 3. 检查日期是否有效（不能预约过去的日期）
        if (date.isBefore(LocalDate.now())) {
            throw new BusinessException(ErrorCode.INVALID_DATE, "不能预约过去的日期");
        }

        // 4. 检查用户当天是否已有有效预约
        LambdaQueryWrapper<Reservation> existQuery = new LambdaQueryWrapper<>();
        existQuery.eq(Reservation::getUserId, userId)
                .eq(Reservation::getDate, date)
                .eq(Reservation::getStatus, ReservationStatusEnum.PENDING.getCode());
        Reservation existReservation = reservationMapper.selectOne(existQuery);
        if (existReservation != null) {
            throw new BusinessException(ErrorCode.ALREADY_RESERVED, "您当天已有待签到的预约");
        }

        // 5. 检查座位状态（使用分布式锁）
        String lockKey = RedisConstant.SEAT_LOCK_KEY + seatId + ":" + date + ":" + periodId;
        boolean locked = redisUtil.tryLock(lockKey, 5, TimeUnit.SECONDS);
        if (!locked) {
            throw new BusinessException(ErrorCode.SEAT_BUSY, "座位正在被其他用户预约，请稍后重试");
        }

        try {
            // 查询或创建座位状态记录
            LambdaQueryWrapper<SeatStatus> statusQuery = new LambdaQueryWrapper<>();
            statusQuery.eq(SeatStatus::getSeatId, seatId)
                    .eq(SeatStatus::getDate, date)
                    .eq(SeatStatus::getPeriodId, periodId);
            SeatStatus seatStatus = seatStatusMapper.selectOne(statusQuery);

            if (seatStatus == null) {
                // 创建座位状态记录
                seatStatus = new SeatStatus();
                seatStatus.setSeatId(seatId);
                seatStatus.setDate(date);
                seatStatus.setPeriodId(periodId);
                seatStatus.setStatus(SeatStatusEnum.AVAILABLE.getCode());
                seatStatus.setCreateTime(LocalDateTime.now());
                seatStatusMapper.insert(seatStatus);
            }

            // 检查座位是否可用
            if (seatStatus.getStatus() != SeatStatusEnum.AVAILABLE.getCode()) {
                throw new BusinessException(ErrorCode.SEAT_NOT_AVAILABLE, "该座位已被预约或不可用");
            }

            // 6. 创建预约记录
            String reservationNo = idGeneratorUtil.generateReservationNo();
            Reservation reservation = new Reservation();
            reservation.setReservationNo(reservationNo);
            reservation.setUserId(userId);
            reservation.setSeatId(seatId);
            reservation.setStudyRoomId(seat.getStudyRoomId());
            reservation.setPeriodId(periodId);
            reservation.setDate(date);
            reservation.setStatus(ReservationStatusEnum.PENDING.getCode());
            
            // 计算签到截止时间（时间段开始时间 + 15分钟）
            LocalTime periodStartTime = period.getStartTime();
            LocalDateTime deadline = LocalDateTime.of(date, periodStartTime).plusMinutes(CHECKIN_TIMEOUT_MINUTES);
            reservation.setCheckinDeadline(deadline);
            
            reservation.setCreateTime(LocalDateTime.now());
            reservation.setUpdateTime(LocalDateTime.now());
            reservationMapper.insert(reservation);

            // 7. 更新座位状态为已预约
            seatStatus.setStatus(SeatStatusEnum.RESERVED.getCode());
            seatStatus.setReservationId(reservation.getId());
            seatStatus.setUpdateTime(LocalDateTime.now());
            seatStatusMapper.updateById(seatStatus);

            // 8. 缓存预约信息（用于签到倒计时）
            String reservationKey = RedisConstant.RESERVATION_TIMEOUT_KEY + reservationNo;
            long ttlSeconds = java.time.Duration.between(LocalDateTime.now(), deadline).getSeconds();
            if (ttlSeconds > 0) {
                redisUtil.set(reservationKey, JSON.toJSONString(reservation), ttlSeconds, TimeUnit.SECONDS);
            }

            // 9. 更新座位缓存
            updateSeatCache(seatId, date, periodId, SeatStatusEnum.RESERVED.getCode());

            log.info("预约成功，reservationNo: {}, userId: {}, seatId: {}", reservationNo, userId, seatId);

            // 10. 构建返回结果
            return buildReservationVO(reservation, seat, period);

        } finally {
            // 释放锁
            redisUtil.unlock(lockKey);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long userId, CancelDTO cancelDTO) {
        Long reservationId = cancelDTO.getReservationId();

        // 1. 查询预约记录
        Reservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null) {
            throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND, "预约记录不存在");
        }

        // 2. 验证是否是用户自己的预约
        if (!reservation.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_YOUR_RESERVATION, "这不是您的预约");
        }

        // 3. 检查预约状态（只能取消待签到的预约）
        if (reservation.getStatus() != ReservationStatusEnum.PENDING.getCode()) {
            throw new BusinessException(ErrorCode.CANNOT_CANCEL, "该预约已签到或已取消，无法取消");
        }

        // 4. 更新预约状态为已取消
        LambdaUpdateWrapper<Reservation> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Reservation::getId, reservationId)
                .set(Reservation::getStatus, ReservationStatusEnum.CANCELLED.getCode())
                .set(Reservation::getUpdateTime, LocalDateTime.now());
        reservationMapper.update(null, updateWrapper);

        // 5. 恢复座位状态
        LambdaUpdateWrapper<SeatStatus> seatStatusUpdate = new LambdaUpdateWrapper<>();
        seatStatusUpdate.eq(SeatStatus::getSeatId, reservation.getSeatId())
                .eq(SeatStatus::getDate, reservation.getDate())
                .eq(SeatStatus::getPeriodId, reservation.getPeriodId())
                .set(SeatStatus::getStatus, SeatStatusEnum.AVAILABLE.getCode())
                .set(SeatStatus::getReservationId, null)
                .set(SeatStatus::getUpdateTime, LocalDateTime.now());
        seatStatusMapper.update(null, seatStatusUpdate);

        // 6. 删除预约缓存
        String reservationKey = RedisConstant.RESERVATION_TIMEOUT_KEY + reservation.getReservationNo();
        redisUtil.delete(reservationKey);

        // 7. 更新座位缓存
        updateSeatCache(reservation.getSeatId(), reservation.getDate(), 
            reservation.getPeriodId(), SeatStatusEnum.AVAILABLE.getCode());

        log.info("取消预约成功，reservationId: {}, userId: {}", reservationId, userId);
    }

    @Override
    public Page<ReservationVO> getMyReservations(Long userId, Integer status, Integer page, Integer size) {
        Page<Reservation> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Reservation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Reservation::getUserId, userId);
        if (status != null) {
            queryWrapper.eq(Reservation::getStatus, status);
        }
        queryWrapper.orderByDesc(Reservation::getCreateTime);

        Page<Reservation> reservationPage = reservationMapper.selectPage(pageParam, queryWrapper);

        // 转换为VO
        Page<ReservationVO> result = new Page<>();
        BeanUtils.copyProperties(reservationPage, result, "records");
        result.setRecords(reservationPage.getRecords().stream()
                .map(r -> {
                    Seat seat = seatMapper.selectById(r.getSeatId());
                    TimePeriod period = timePeriodMapper.selectById(r.getPeriodId());
                    return buildReservationVO(r, seat, period);
                })
                .collect(java.util.stream.Collectors.toList()));

        return result;
    }

    @Override
    public ReservationVO getReservationDetail(Long userId, Long reservationId) {
        Reservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null) {
            throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND, "预约记录不存在");
        }

        if (!reservation.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_YOUR_RESERVATION, "这不是您的预约");
        }

        Seat seat = seatMapper.selectById(reservation.getSeatId());
        TimePeriod period = timePeriodMapper.selectById(reservation.getPeriodId());

        return buildReservationVO(reservation, seat, period);
    }

    @Override
    public ReservationVO getCurrentReservation(Long userId) {
        LambdaQueryWrapper<Reservation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Reservation::getUserId, userId)
                .eq(Reservation::getStatus, ReservationStatusEnum.PENDING.getCode())
                .ge(Reservation::getDate, LocalDate.now())
                .orderByAsc(Reservation::getDate)
                .last("LIMIT 1");

        Reservation reservation = reservationMapper.selectOne(queryWrapper);
        if (reservation == null) {
            return null;
        }

        Seat seat = seatMapper.selectById(reservation.getSeatId());
        TimePeriod period = timePeriodMapper.selectById(reservation.getPeriodId());

        return buildReservationVO(reservation, seat, period);
    }

    @Override
    public Page<ReservationVO> getHistory(Long userId, LocalDate startDate, LocalDate endDate, Integer page, Integer size) {
        Page<Reservation> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Reservation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Reservation::getUserId, userId)
                .between(Reservation::getDate, startDate, endDate)
                .ne(Reservation::getStatus, ReservationStatusEnum.PENDING.getCode())
                .orderByDesc(Reservation::getCreateTime);

        Page<Reservation> reservationPage = reservationMapper.selectPage(pageParam, queryWrapper);

        Page<ReservationVO> result = new Page<>();
        BeanUtils.copyProperties(reservationPage, result, "records");
        result.setRecords(reservationPage.getRecords().stream()
                .map(r -> {
                    Seat seat = seatMapper.selectById(r.getSeatId());
                    TimePeriod period = timePeriodMapper.selectById(r.getPeriodId());
                    return buildReservationVO(r, seat, period);
                })
                .collect(java.util.stream.Collectors.toList()));

        return result;
    }

    @Override
    public Boolean checkAvailable(Long seatId, LocalDate date, Long periodId) {
        LambdaQueryWrapper<SeatStatus> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SeatStatus::getSeatId, seatId)
                .eq(SeatStatus::getDate, date)
                .eq(SeatStatus::getPeriodId, periodId);
        SeatStatus seatStatus = seatStatusMapper.selectOne(queryWrapper);

        if (seatStatus == null) {
            return true;
        }

        return seatStatus.getStatus() == SeatStatusEnum.AVAILABLE.getCode();
    }

    /**
     * 更新座位缓存
     */
    private void updateSeatCache(Long seatId, LocalDate date, Long periodId, Integer status) {
        String cacheKey = RedisConstant.SEAT_STATUS_KEY + date + ":" + periodId;
        redisUtil.hset(cacheKey, seatId.toString(), status.toString());
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
