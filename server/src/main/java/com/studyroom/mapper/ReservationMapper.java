package com.studyroom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studyroom.entity.Reservation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface ReservationMapper extends BaseMapper<Reservation> {

    @Select("SELECT * FROM reservation WHERE user_id = #{userId} AND status = 1 AND date >= #{today} ORDER BY create_time DESC")
    List<Reservation> selectCurrentReservations(@Param("userId") Long userId, @Param("today") String today);

    @Select("SELECT * FROM reservation WHERE user_id = #{userId} AND status IN (2,3,4) ORDER BY create_time DESC LIMIT #{offset}, #{size}")
    List<Reservation> selectHistoryReservations(@Param("userId") Long userId,
                                                @Param("offset") Integer offset,
                                                @Param("size") Integer size);

    @Select("SELECT COUNT(*) FROM reservation WHERE user_id = #{userId} AND status IN (2,3,4)")
    Integer countHistory(@Param("userId") Long userId);
}