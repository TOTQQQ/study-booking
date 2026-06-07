package com.studyroom.service.impl;

import com.studyroom.dto.QuerySeatDTO;
import com.studyroom.entity.Seat;
import com.studyroom.entity.SeatStatus;
import com.studyroom.entity.TimePeriod;
import com.studyroom.enums.SeatStatusEnum;
import com.studyroom.exception.BusinessException;
import com.studyroom.exception.ErrorCode;
import com.studyroom.mapper.SeatMapper;
import com.studyroom.mapper.SeatStatusMapper;
import com.studyroom.mapper.TimePeriodMapper;
import com.studyroom.service.SeatService;
import com.studyroom.utils.DateUtils;
import com.studyroom.vo.SeatVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SeatServiceImpl implements SeatService {

    private static final Logger log = LoggerFactory.getLogger(SeatServiceImpl.class);

    private final SeatMapper seatMapper;
    private final SeatStatusMapper seatStatusMapper;
    private final TimePeriodMapper timePeriodMapper;
    private final SeatCacheService seatCacheService; // ← 加这一行

    public SeatServiceImpl(SeatMapper seatMapper,
            SeatStatusMapper seatStatusMapper,
            TimePeriodMapper timePeriodMapper,
            SeatCacheService seatCacheService) { // ← 加这个参数
        this.seatMapper = seatMapper;
        this.seatStatusMapper = seatStatusMapper;
        this.timePeriodMapper = timePeriodMapper;
        this.seatCacheService = seatCacheService; // ← 加这一行
    }

    @Override
    public List<SeatVO> querySeatStatus(QuerySeatDTO dto) {
        // 1. 参数校验
        if (dto.getRoomId() == null || dto.getDate() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }

        // 2. 校验日期范围（未来1-3天）
        long daysDiff = DateUtils.daysBetween(DateUtils.getToday(), dto.getDate());
        if (daysDiff < 0 || daysDiff > 3) {
            throw new BusinessException(ErrorCode.INVALID_DATE);
        }

        // 3. 查询座位（按自习室过滤）
        List<Seat> seats = seatMapper.selectByRoomId(dto.getRoomId());
        if (seats.isEmpty()) {
            return new ArrayList<>();
        }

        // 4. 查询时间段
        List<TimePeriod> timePeriods = timePeriodMapper.selectAllEnabled();

        // ========== 5. 查询座位状态（优先从缓存读取）==========
        List<SeatStatus> statusList;

        // 先从Redis缓存读取
        Map<String, Integer> cachedMap = seatCacheService.getSeatStatusFromCache(dto.getDate(), dto.getRoomId());

        if (cachedMap != null && !cachedMap.isEmpty()) {
            // 缓存命中：把缓存数据转换成 List<SeatStatus>
            statusList = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : cachedMap.entrySet()) {
                String[] parts = entry.getKey().split("_");
                if (parts.length == 2) {
                    SeatStatus ss = new SeatStatus();
                    ss.setSeatId(Integer.parseInt(parts[0]));
                    ss.setTimePeriodId(Integer.parseInt(parts[1]));
                    ss.setStatus(entry.getValue());
                    ss.setDate(dto.getDate());
                    ss.setRoomId(dto.getRoomId());
                    statusList.add(ss);
                }
            }
            log.info("从缓存读取座位状态: roomId={}, date={}", dto.getRoomId(), dto.getDate());
        } else {
            // 缓存未命中：查数据库
            statusList = seatStatusMapper.selectByDateAndRoom(dto.getDate(), dto.getRoomId());

            // 把数据库结果写入缓存
            if (statusList != null && !statusList.isEmpty()) {
                Map<String, Integer> mapToCache = new HashMap<>();
                for (SeatStatus ss : statusList) {
                    String key = ss.getSeatId() + "_" + ss.getTimePeriodId();
                    mapToCache.put(key, ss.getStatus());
                }
                seatCacheService.setSeatStatusToCache(dto.getDate(), dto.getRoomId(), mapToCache);
            }
            log.info("从数据库读取座位状态并写入缓存: roomId={}, date={}", dto.getRoomId(), dto.getDate());
        }
        // ========== 缓存读取结束 ==========

        // 6. 组装返回数据（这部分保持不变）
        Map<String, Integer> statusMap = statusList.stream()
                .collect(Collectors.toMap(
                        ss -> ss.getSeatId() + "_" + ss.getTimePeriodId(),
                        SeatStatus::getStatus));

        List<SeatVO> result = new ArrayList<>();
        for (Seat seat : seats) {
            SeatVO vo = new SeatVO();
            vo.setId(seat.getId());
            vo.setSeatCode(seat.getSeatCode());
            vo.setX(seat.getX());
            vo.setY(seat.getY());

            List<SeatVO.TimeSlotStatus> timeSlots = new ArrayList<>();
            for (TimePeriod tp : timePeriods) {
                SeatVO.TimeSlotStatus slot = new SeatVO.TimeSlotStatus();
                slot.setTimePeriodId(tp.getId());
                slot.setStartTime(tp.getStartTime());
                slot.setEndTime(tp.getEndTime());

                String key = seat.getId() + "_" + tp.getId();
                Integer status = statusMap.get(key);
                slot.setStatus(status != null ? status : SeatStatusEnum.AVAILABLE.getCode());
                timeSlots.add(slot);
            }
            vo.setTimeSlots(timeSlots);

            if (dto.getTimePeriodId() != null) {
                String key = seat.getId() + "_" + dto.getTimePeriodId();
                vo.setStatus(statusMap.getOrDefault(key, SeatStatusEnum.AVAILABLE.getCode()));
            }

            result.add(vo);
        }

        log.info("查询座位状态成功, roomId={}, date={}, 返回{}条记录", dto.getRoomId(), dto.getDate(), result.size());
        return result;
    }

    @Override
    public void updateSeatStatus(Integer seatId, String date, Integer timePeriodId, Integer status) {
        // 1. 先查询座位信息（为了拿到roomId，用于更新缓存）
        Seat seat = seatMapper.selectById(seatId);
        if (seat == null) {
            throw new BusinessException(ErrorCode.SEAT_NOT_FOUND);
        }

        // 2. 更新数据库
        SeatStatus seatStatus = seatStatusMapper.selectBySeatDatePeriod(seatId, date, timePeriodId);

        if (seatStatus == null) {
            seatStatus = new SeatStatus();
            seatStatus.setSeatId(seatId);
            seatStatus.setRoomId(seat.getRoomId());
            seatStatus.setDate(date);
            seatStatus.setTimePeriodId(timePeriodId);
            seatStatus.setStatus(status);
            seatStatus.setCreateTime(System.currentTimeMillis());
            seatStatusMapper.insert(seatStatus);
        } else {
            seatStatus.setStatus(status);
            seatStatusMapper.updateById(seatStatus);
        }

        // 3. 同步更新Redis缓存
        seatCacheService.updateSingleSeatStatus(date, seat.getRoomId(), seatId, timePeriodId, status);

        log.info("更新座位状态: seatId={}, date={}, timePeriodId={}, status={}", seatId, date, timePeriodId, status);
    }
}