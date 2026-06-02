package com.studyroom.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 时间段实体类 - 对应数据库 timePeriod 表
 */
@Data
@TableName("timePeriod")
public class TimePeriod {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String startTime;

    private String endTime;

    private Integer sort;

    private Integer status;

    private Long createTime;

    private Long updateTime;
}
