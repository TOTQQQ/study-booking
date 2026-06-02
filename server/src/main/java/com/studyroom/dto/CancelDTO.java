package com.studyroom.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 取消预约请求DTO - 对齐数据库表结构
 * 负责人：严雯琪
 */
@Data
@ApiModel("取消预约请求参数")
public class CancelDTO {

    @ApiModelProperty(value = "预约ID", required = true)
    @NotNull(message = "预约ID不能为空")
    private Integer reservationId;

    @ApiModelProperty("取消原因")
    private String reason;
}
