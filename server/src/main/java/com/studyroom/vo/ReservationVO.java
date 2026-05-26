package com.studyroom.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 预约响应VO
 * 负责人：严雯琪
 */
@Data
@ApiModel("预约响应结果")
public class ReservationVO {

    @ApiModelProperty("预约ID")
    private Long id;

    @ApiModelProperty("预约编号")
    private String reservationNo;

    @ApiModelProperty("用户ID")
    private Long userId;

    @ApiModelProperty("座位ID")
    private Long seatId;

    @ApiModelProperty("座位编号")
    private String seatNo;

    @ApiModelProperty("座位行号")
    private Integer seatRow;

    @ApiModelProperty("座位列号")
    private Integer seatCol;

    @ApiModelProperty("自习室ID")
    private Long studyRoomId;

    @ApiModelProperty("时间段ID")
    private Long periodId;

    @ApiModelProperty("时间段名称")
    private String periodName;

    @ApiModelProperty("开始时间")
    private LocalTime startTime;

    @ApiModelProperty("结束时间")
    private LocalTime endTime;

    @ApiModelProperty("预约日期")
    private LocalDate date;

    @ApiModelProperty("预约状态：1-待签到，2-已签到，3-已取消，4-自动取消")
    private Integer status;

    @ApiModelProperty("签到截止时间")
    private LocalDateTime checkinDeadline;

    @ApiModelProperty("签到时间")
    private LocalDateTime checkinTime;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty("更新时间")
    private LocalDateTime updateTime;
}
