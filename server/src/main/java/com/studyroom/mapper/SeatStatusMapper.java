package com.studyroom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studyroom.entity.SeatStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 座位状态Mapper接口 - 对齐数据库 seatStatus 表
 */
@Mapper
public interface SeatStatusMapper extends BaseMapper<SeatStatus> {

    @Select("SELECT * FROM seatStatus WHERE date = #{date} AND roomId = #{roomId}")
    List<SeatStatus> selectByDateAndRoom(@Param("date") String date,
                                         @Param("roomId") Integer roomId);

    @Select("SELECT * FROM seatStatus WHERE seatId = #{seatId} AND date = #{date} AND timePeriodId = #{timePeriodId}")
    SeatStatus selectBySeatDatePeriod(@Param("seatId") Integer seatId,
                                      @Param("date") String date,
                                      @Param("timePeriodId") Integer timePeriodId);
}
