package com.studyroom.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 座位实体类 - 对应数据库 seat 表
 */
@Data
@TableName("seat")
public class Seat {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer roomId;

    private String seatCode;

    private Integer x;

    private Integer y;

    private Integer status;

    private Long createTime;

    private Long updateTime;
}
