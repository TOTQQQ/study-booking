package com.studyroom.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 预约响应VO - 对齐数据库表结构
 * 负责人：严雯琪
 */
@Data
@ApiModel("预约响应结果")
public class ReservationVO {

    @ApiModelProperty("预约ID")
    private Integer id;

    @ApiModelProperty("预约单号")
    private String reserveNo;

    @ApiModelProperty("用户ID")
    private Integer userId;

    @ApiModelProperty("座位ID")
    private Integer seatId;

    @ApiModelProperty("座位编号，如A01")
    private String seatCode;

    @ApiModelProperty("座位X坐标")
    private Integer seatX;

    @ApiModelProperty("座位Y坐标")
    private Integer seatY;

    @ApiModelProperty("自习室ID")
    private Integer roomId;

    @ApiModelProperty("自习室名称")
    private String roomName;

    @ApiModelProperty("时间段ID")
    private Integer timePeriodId;

    @ApiModelProperty("开始时间，如08:00")
    private String startTime;

    @ApiModelProperty("结束时间，如12:00")
    private String endTime;

    @ApiModelProperty("预约日期，如2024-01-01")
    private String date;

    @ApiModelProperty("预约状态：1-待签到，2-已签到，3-已取消，4-自动取消")
    private Integer status;

    @ApiModelProperty("签到时间（时间戳）")
    private Long signTime;

    @ApiModelProperty("取消时间（时间戳）")
    private Long cancelTime;

    @ApiModelProperty("创建时间（时间戳）")
    private Long createTime;

    @ApiModelProperty("更新时间（时间戳）")
    private Long updateTime;
}
