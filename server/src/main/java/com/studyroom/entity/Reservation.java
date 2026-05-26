package com.studyroom.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 预约记录实体类
 */
@Data
@TableName("reservation")
public class Reservation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String reservationNo;

    private Long userId;

    private Long seatId;

    private Long studyRoomId;

    private Long periodId;

    private LocalDate date;

    private Integer status;

    private LocalDateTime checkinDeadline;

    private LocalDateTime checkinTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
