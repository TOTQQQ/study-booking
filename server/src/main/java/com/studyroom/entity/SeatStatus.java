package com.studyroom.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 座位状态实体类
 */
@Data
@TableName("seat_status")
public class SeatStatus {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long seatId;

    private LocalDate date;

    private Long periodId;

    private Integer status;

    private Long reservationId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
