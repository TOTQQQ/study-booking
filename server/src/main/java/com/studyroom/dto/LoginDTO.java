package com.studyroom.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 登录请求DTO
 * 负责人：严雯琪
 */
@Data
@ApiModel("登录请求参数")
public class LoginDTO {

    @ApiModelProperty(value = "微信登录code", required = true)
    @NotBlank(message = "微信登录code不能为空")
    private String code;

    @ApiModelProperty("用户昵称")
    private String nickname;

    @ApiModelProperty("用户头像URL")
    private String avatar;
}
