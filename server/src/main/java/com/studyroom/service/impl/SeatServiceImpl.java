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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SeatServiceImpl implements SeatService {
    
    private static final Logger log = LoggerFactory.getLogger(SeatServiceImpl.class);
    
    private final SeatMapper seatMapper;
    private final SeatStatusMapper seatStatusMapper;
    private final TimePeriodMapper timePeriodMapper;
    
    public SeatServiceImpl(SeatMapper seatMapper,
                           SeatStatusMapper seatStatusMapper,
                           TimePeriodMapper timePeriodMapper) {
        this.seatMapper = seatMapper;
        this.seatStatusMapper = seatStatusMapper;
        this.timePeriodMapper = timePeriodMapper;
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
        
        // 5. 查询座位状态
        List<SeatStatus> statusList = seatStatusMapper.selectByDateAndRoom(dto.getDate(), dto.getRoomId());
        Map<String, Integer> statusMap = statusList.stream()
            .collect(Collectors.toMap(
                ss -> ss.getSeatId() + "_" + ss.getTimePeriodId(),
                SeatStatus::getStatus
            ));
        
        // 6. 组装返回数据
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
            
            // 如果指定了时间段，设置当前状态
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
        // 查询是否存在
        SeatStatus seatStatus = seatStatusMapper.selectBySeatDatePeriod(seatId, date, timePeriodId);
        
        if (seatStatus == null) {
            // 不存在则新增
            seatStatus = new SeatStatus();
            seatStatus.setSeatId(seatId);
            seatStatus.setDate(date);
            seatStatus.setTimePeriodId(timePeriodId);
            seatStatus.setStatus(status);
            seatStatus.setCreateTime(System.currentTimeMillis());
            seatStatusMapper.insert(seatStatus);
        } else {
            // 存在则更新
            seatStatus.setStatus(status);
            seatStatusMapper.updateById(seatStatus);
        }
        
        log.info("更新座位状态: seatId={}, date={}, timePeriodId={}, status={}", seatId, date, timePeriodId, status);
    }
}