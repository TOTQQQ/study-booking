package com.studyroom.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 座位每日状态实体类 - 对应数据库 seatStatus 表
 */
@Data
@TableName("seatStatus")
public class SeatStatus {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer seatId;

    private Integer roomId;

    private String date;

    private Integer timePeriodId;

    private Integer status;

    private Long createTime;

    private Long updateTime;
}
