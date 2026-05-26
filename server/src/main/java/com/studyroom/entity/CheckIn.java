package com.studyroom.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 签到记录实体类
 */
@Data
@TableName("check_in")
public class CheckIn {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long reservationId;

    private Long userId;

    private Long seatId;

    private LocalDateTime checkinTime;

    private Integer checkinType;

    private LocalDateTime createTime;
}
