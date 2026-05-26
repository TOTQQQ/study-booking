package com.studyroom.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 预约请求DTO
 * 负责人：严雯琪
 */
@Data
@ApiModel("预约请求参数")
public class ReserveDTO {

    @ApiModelProperty(value = "座位ID", required = true)
    @NotNull(message = "座位ID不能为空")
    private Long seatId;

    @ApiModelProperty(value = "预约日期", required = true)
    @NotNull(message = "预约日期不能为空")
    private LocalDate date;

    @ApiModelProperty(value = "时间段ID", required = true)
    @NotNull(message = "时间段ID不能为空")
    private Long periodId;
}
