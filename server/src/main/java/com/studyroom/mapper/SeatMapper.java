package com.studyroom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studyroom.entity.Seat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 座位Mapper接口
 */
@Mapper
public interface SeatMapper extends BaseMapper<Seat> {

    @Select("SELECT * FROM seat WHERE roomId = #{roomId} AND status = 1")
    List<Seat> selectByRoomId(@Param("roomId") Integer roomId);
}
