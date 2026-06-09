package com.studyroom.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.studyroom.cache.SeatCacheService;
import com.studyroom.constant.RedisConstant;
import com.studyroom.dto.CancelDTO;
import com.studyroom.dto.ReserveDTO;
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
import com.studyroom.service.ReservationService;
import com.studyroom.utils.IdGeneratorUtil;
import com.studyroom.utils.RedisUtil;
import com.studyroom.vo.ReservationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 预约服务实现类 - 对齐数据库表结构
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
    private final StudyRoomMapper studyRoomMapper;
    private final RedisUtil redisUtil;
    private final SeatCacheService seatCacheService;
    private final IdGeneratorUtil idGeneratorUtil;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReservationVO reserve(Long userId, ReserveDTO reserveDTO) {
        Integer seatId = reserveDTO.getSeatId();
        String date = reserveDTO.getDate();
        Integer timePeriodId = reserveDTO.getTimePeriodId();

        // 1. 检查座位是否存在
        Seat seat = seatMapper.selectById(seatId);
        if (seat == null) {
            throw new BusinessException(ErrorCode.SEAT_NOT_FOUND, "座位不存在");
        }

        // 2. 检查时间段是否存在且启用
        TimePeriod period = timePeriodMapper.selectById(timePeriodId);
        if (period == null || period.getStatus() != 1) {
            throw new BusinessException(ErrorCode.PERIOD_NOT_FOUND, "时间段不存在或已禁用");
        }

        // 3. 检查日期是否有效（不能预约过去的日期）
        LocalDate dateObj = LocalDate.parse(date, DATE_FMT);
        if (dateObj.isBefore(LocalDate.now())) {
            throw new BusinessException(ErrorCode.INVALID_DATE, "不能预约过去的日期");
        }

        // 4. 检查用户当天是否已有待签到预约
        LambdaQueryWrapper<Reservation> existQuery = new LambdaQueryWrapper<>();
        existQuery.eq(Reservation::getUserId, userId.intValue())
                .eq(Reservation::getDate, date)
                .eq(Reservation::getStatus, ReservationStatusEnum.PENDING.getCode());
        if (reservationMapper.selectCount(existQuery) > 0) {
            throw new BusinessException(ErrorCode.ALREADY_RESERVED, "您当天已有待签到的预约");
        }

        // 5. 分布式锁防止并发预约
        String lockKey = RedisConstant.SEAT_LOCK_KEY + seatId + ":" + date + ":" + timePeriodId;
        boolean locked = redisUtil.tryLock(lockKey, 5, TimeUnit.SECONDS);
        if (!locked) {
            throw new BusinessException(ErrorCode.SEAT_BUSY, "座位正在被其他用户预约，请稍后重试");
        }

        try {
            // 查询或创建座位状态记录
            LambdaQueryWrapper<SeatStatus> statusQuery = new LambdaQueryWrapper<>();
            statusQuery.eq(SeatStatus::getSeatId, seatId)
                    .eq(SeatStatus::getDate, date)
                    .eq(SeatStatus::getTimePeriodId, timePeriodId);
            SeatStatus seatStatus = seatStatusMapper.selectOne(statusQuery);

            if (seatStatus == null) {
                seatStatus = new SeatStatus();
                seatStatus.setSeatId(seatId);
                seatStatus.setRoomId(seat.getRoomId());
                seatStatus.setDate(date);
                seatStatus.setTimePeriodId(timePeriodId);
                seatStatus.setStatus(SeatStatusEnum.AVAILABLE.getCode());
                seatStatus.setCreateTime(System.currentTimeMillis());
                seatStatusMapper.insert(seatStatus);
            }

            if (seatStatus.getStatus() != SeatStatusEnum.AVAILABLE.getCode()) {
                throw new BusinessException(ErrorCode.SEAT_NOT_AVAILABLE, "该座位已被预约或不可用");
            }

            // 6. 创建预约记录
            String reserveNo = idGeneratorUtil.generateReservationNo();
            long now = System.currentTimeMillis();

            Reservation reservation = new Reservation();
            reservation.setReserveNo(reserveNo);
            reservation.setUserId(userId.intValue());
            reservation.setSeatId(seatId);
            reservation.setRoomId(seat.getRoomId());
            reservation.setTimePeriodId(timePeriodId);
            reservation.setDate(date);
            reservation.setStatus(ReservationStatusEnum.PENDING.getCode());
            reservation.setSignTime(0L);
            reservation.setCancelTime(0L);
            reservation.setCreateTime(now);
            reservation.setUpdateTime(now);
            reservationMapper.insert(reservation);

            // 7. 更新座位状态为已预约
            seatStatus.setStatus(SeatStatusEnum.RESERVED.getCode());
            seatStatus.setUpdateTime(now);
            seatStatusMapper.updateById(seatStatus);

            // 8. 更新座位缓存
            seatCacheService.updateSingleSeatStatus(date, seat.getRoomId(), seatId, timePeriodId,
                    SeatStatusEnum.RESERVED.getCode());

            log.info("预约成功，reserveNo: {}, userId: {}, seatId: {}", reserveNo, userId, seatId);

            return buildReservationVO(reservation, seat, period);

        } finally {
            redisUtil.unlock(lockKey);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long userId, CancelDTO cancelDTO) {
        Integer reservationId = cancelDTO.getReservationId();

        Reservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null) {
            throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND, "预约记录不存在");
        }

        if (!reservation.getUserId().equals(userId.intValue())) {
            throw new BusinessException(ErrorCode.NOT_YOUR_RESERVATION, "这不是您的预约");
        }

        if (reservation.getStatus() != ReservationStatusEnum.PENDING.getCode()) {
            throw new BusinessException(ErrorCode.CANNOT_CANCEL, "该预约已签到或已取消，无法取消");
        }

        long now = System.currentTimeMillis();

        // 更新预约状态
        LambdaUpdateWrapper<Reservation> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Reservation::getId, reservationId)
                .set(Reservation::getStatus, ReservationStatusEnum.CANCELLED.getCode())
                .set(Reservation::getCancelTime, now)
                .set(Reservation::getUpdateTime, now);
        reservationMapper.update(null, updateWrapper);

        // 恢复座位状态
        LambdaUpdateWrapper<SeatStatus> seatStatusUpdate = new LambdaUpdateWrapper<>();
        seatStatusUpdate.eq(SeatStatus::getSeatId, reservation.getSeatId())
                .eq(SeatStatus::getDate, reservation.getDate())
                .eq(SeatStatus::getTimePeriodId, reservation.getTimePeriodId())
                .set(SeatStatus::getStatus, SeatStatusEnum.AVAILABLE.getCode())
                .set(SeatStatus::getUpdateTime, now);
        seatStatusMapper.update(null, seatStatusUpdate);

        // 更新座位缓存
        seatCacheService.updateSingleSeatStatus(reservation.getDate(), reservation.getRoomId(),
                reservation.getSeatId(), reservation.getTimePeriodId(), SeatStatusEnum.AVAILABLE.getCode());

        log.info("取消预约成功，reservationId: {}, userId: {}", reservationId, userId);
    }

    @Override
    public Page<ReservationVO> getMyReservations(Long userId, Integer status, Integer page, Integer size) {
        Page<Reservation> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Reservation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Reservation::getUserId, userId.intValue());
        if (status != null) {
            queryWrapper.eq(Reservation::getStatus, status);
        }
        queryWrapper.orderByDesc(Reservation::getCreateTime);

        Page<Reservation> reservationPage = reservationMapper.selectPage(pageParam, queryWrapper);

        Page<ReservationVO> result = new Page<>(reservationPage.getCurrent(), reservationPage.getSize(), reservationPage.getTotal());
        result.setRecords(reservationPage.getRecords().stream()
                .map(r -> {
                    Seat seat = seatMapper.selectById(r.getSeatId());
                    TimePeriod period = timePeriodMapper.selectById(r.getTimePeriodId());
                    return buildReservationVO(r, seat, period);
                })
                .collect(Collectors.toList()));

        return result;
    }

    @Override
    public ReservationVO getReservationDetail(Long userId, Long reservationId) {
        Reservation reservation = reservationMapper.selectById(reservationId.intValue());
        if (reservation == null) {
            throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND, "预约记录不存在");
        }
        if (!reservation.getUserId().equals(userId.intValue())) {
            throw new BusinessException(ErrorCode.NOT_YOUR_RESERVATION, "这不是您的预约");
        }

        Seat seat = seatMapper.selectById(reservation.getSeatId());
        TimePeriod period = timePeriodMapper.selectById(reservation.getTimePeriodId());
        return buildReservationVO(reservation, seat, period);
    }

    @Override
    public ReservationVO getCurrentReservation(Long userId) {
        String today = LocalDate.now().format(DATE_FMT);

        LambdaQueryWrapper<Reservation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Reservation::getUserId, userId.intValue())
                .eq(Reservation::getStatus, ReservationStatusEnum.PENDING.getCode())
                .ge(Reservation::getDate, today)
                .orderByAsc(Reservation::getDate)
                .last("LIMIT 1");

        Reservation reservation = reservationMapper.selectOne(queryWrapper);
        if (reservation == null) {
            return null;
        }

        Seat seat = seatMapper.selectById(reservation.getSeatId());
        TimePeriod period = timePeriodMapper.selectById(reservation.getTimePeriodId());
        return buildReservationVO(reservation, seat, period);
    }

    @Override
    public Page<ReservationVO> getHistory(Long userId, String startDate, String endDate, Integer page, Integer size) {
        Page<Reservation> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Reservation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Reservation::getUserId, userId.intValue())
                .between(Reservation::getDate, startDate, endDate)
                .ne(Reservation::getStatus, ReservationStatusEnum.PENDING.getCode())
                .orderByDesc(Reservation::getCreateTime);

        Page<Reservation> reservationPage = reservationMapper.selectPage(pageParam, queryWrapper);

        Page<ReservationVO> result = new Page<>(reservationPage.getCurrent(), reservationPage.getSize(), reservationPage.getTotal());
        result.setRecords(reservationPage.getRecords().stream()
                .map(r -> {
                    Seat seat = seatMapper.selectById(r.getSeatId());
                    TimePeriod period = timePeriodMapper.selectById(r.getTimePeriodId());
                    return buildReservationVO(r, seat, period);
                })
                .collect(Collectors.toList()));

        return result;
    }

    @Override
    public Boolean checkAvailable(Integer seatId, String date, Integer timePeriodId) {
        LambdaQueryWrapper<SeatStatus> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SeatStatus::getSeatId, seatId)
                .eq(SeatStatus::getDate, date)
                .eq(SeatStatus::getTimePeriodId, timePeriodId);
        SeatStatus seatStatus = seatStatusMapper.selectOne(queryWrapper);

        if (seatStatus == null) {
            return true;
        }
        return seatStatus.getStatus() == SeatStatusEnum.AVAILABLE.getCode();
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
