package com.studyroom.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 登录响应VO
 * 负责人：严雯琪
 */
@Data
@ApiModel("登录响应结果")
public class LoginVO {

    @ApiModelProperty("用户ID")
    private Long userId;

    @ApiModelProperty("微信OpenID")
    private String openid;

    @ApiModelProperty("用户昵称")
    private String nickname;

    @ApiModelProperty("用户头像URL")
    private String avatar;

    @ApiModelProperty("访问令牌")
    private String accessToken;

    @ApiModelProperty("刷新令牌")
    private String refreshToken;

    @ApiModelProperty("令牌过期时间（秒）")
    private Long expiresIn;

    @ApiModelProperty("是否新用户")
    private Boolean isNewUser;
}
