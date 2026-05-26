package com.studyroom.mapper;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studyroom.entity.SeatStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 座位状态Mapper接口
 */
@Mapper
public interface SeatStatusMapper extends BaseMapper<SeatStatus> {

    @Update("UPDATE seat_status SET status = #{status}, reservation_id = #{reservationId}, update_time = NOW() " +
            "WHERE seat_id = #{seatId} AND date = #{date} AND period_id = #{periodId}")
    int updateStatus(@Param("seatId") Long seatId, @Param("date") String date,
                     @Param("periodId") Long periodId, @Param("status") Integer status,
                     @Param("reservationId") Long reservationId);
}
