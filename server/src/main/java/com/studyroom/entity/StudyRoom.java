package com.studyroom.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 自习室实体类 - 对应数据库 studyRoom 表
 */
@Data
@TableName("studyRoom")
public class StudyRoom {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String roomName;

    private String location;

    private Integer status;

    private Long createTime;

    private Long updateTime;
}
