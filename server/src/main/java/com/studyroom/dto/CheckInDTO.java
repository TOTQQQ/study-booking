package com.studyroom.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 签到请求DTO - 对齐数据库表结构
 * 负责人：严雯琪
 */
@Data
@ApiModel("签到请求参数")
public class CheckInDTO {

    @ApiModelProperty(value = "预约ID", required = true)
    @NotNull(message = "预约ID不能为空")
    private Integer reservationId;
}
