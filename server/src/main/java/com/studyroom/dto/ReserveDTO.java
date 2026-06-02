package com.studyroom.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 预约请求DTO - 对齐数据库表结构
 * 负责人：严雯琪
 */
@Data
@ApiModel("预约请求参数")
public class ReserveDTO {

    @ApiModelProperty(value = "座位ID", required = true)
    @NotNull(message = "座位ID不能为空")
    private Integer seatId;

    @ApiModelProperty(value = "预约日期，格式yyyy-MM-dd", required = true)
    @NotBlank(message = "预约日期不能为空")
    private String date;

    @ApiModelProperty(value = "时间段ID", required = true)
    @NotNull(message = "时间段ID不能为空")
    private Integer timePeriodId;
}
