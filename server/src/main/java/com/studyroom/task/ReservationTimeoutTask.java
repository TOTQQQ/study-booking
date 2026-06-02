package com.studyroom.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.studyroom.constant.SystemConstant;
import com.studyroom.entity.Reservation;
import com.studyroom.entity.TimePeriod;
import com.studyroom.enums.ReservationStatusEnum;
import com.studyroom.mapper.ReservationMapper;
import com.studyroom.mapper.TimePeriodMapper;
import com.studyroom.service.SeatService;
import com.studyroom.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@EnableScheduling
public class ReservationTimeoutTask {
    
    private static final Logger log = LoggerFactory.getLogger(ReservationTimeoutTask.class);
    
    private final ReservationMapper reservationMapper;
    private final TimePeriodMapper timePeriodMapper;
    private final SeatService seatService;
    
    // 手动添加构造函数
    public ReservationTimeoutTask(ReservationMapper reservationMapper,
                                  TimePeriodMapper timePeriodMapper,
                                  SeatService seatService) {
        this.reservationMapper = reservationMapper;
        this.timePeriodMapper = timePeriodMapper;
        this.seatService = seatService;
    }
    
    @Scheduled(cron = "0 */5 * * * ?")
    @Transactional
    public void cancelTimeoutReservations() {
        log.info("开始执行超时自动取消定时任务");
        
        String today = DateUtils.getToday();
        
        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Reservation::getStatus, ReservationStatusEnum.PENDING.getCode())
               .eq(Reservation::getDate, today);
        
        List<Reservation> reservations = reservationMapper.selectList(wrapper);
        log.info("查询到待签到预约 {} 条", reservations.size());
        
        int cancelCount = 0;
        
        for (Reservation reservation : reservations) {
            TimePeriod timePeriod = timePeriodMapper.selectById(reservation.getTimePeriodId());
            if (timePeriod == null) {
                continue;
            }
            
            boolean isTimeout = DateUtils.isTimeout(
                reservation.getDate(), 
                timePeriod.getStartTime(), 
                SystemConstant.TIMEOUT_MINUTES
            );
            
            if (isTimeout) {
                reservation.setStatus(ReservationStatusEnum.AUTO_CANCELLED.getCode());
                reservation.setCancelTime(DateUtils.getCurrentTimestamp());
                reservationMapper.updateById(reservation);
                
                seatService.updateSeatStatus(
                    reservation.getSeatId(),
                    reservation.getDate(),
                    reservation.getTimePeriodId(),
                    1
                );
                
                cancelCount++;
                log.info("自动取消预约: reserveNo={}, userId={}", reservation.getReserveNo(), reservation.getUserId());
            }
        }
        
        log.info("定时任务完成，共自动取消 {} 条预约", cancelCount);
    }
}