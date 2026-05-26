package com.studyroom.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 座位实体类
 */
@Data
@TableName("seat")
public class Seat {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studyRoomId;

    private String seatNo;

    private Integer rowNum;

    private Integer colNum;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
