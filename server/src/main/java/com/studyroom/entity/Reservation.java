package com.studyroom.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 预约记录实体类 - 对应数据库 reservation 表
 */
@Data
@TableName("reservation")
public class Reservation {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String reserveNo;

    private Integer userId;

    private Integer seatId;

    private Integer roomId;

    private String date;

    private Integer timePeriodId;

    private Integer status;

    private Long signTime;

    private Long cancelTime;

    private Long createTime;

    private Long updateTime;


}
