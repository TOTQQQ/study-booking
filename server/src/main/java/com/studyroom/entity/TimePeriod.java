package com.studyroom.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalTime;

/**
 * 时间段实体类
 */
@Data
@TableName("time_period")
public class TimePeriod {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String periodName;

    private LocalTime startTime;

    private LocalTime endTime;

    private Integer sort;
}
