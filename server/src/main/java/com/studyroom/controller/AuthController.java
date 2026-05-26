package com.studyroom.controller;

import com.studyroom.common.Result;
import com.studyroom.dto.LoginDTO;
import com.studyroom.service.AuthService;
import com.studyroom.vo.LoginVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器 - 微信登录、JWT令牌管理
 * 负责人：严雯琪
 * 任务ID：1.3 用户微信授权登录接口开发
 */
@Api(tags = "认证管理")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * 微信小程序登录
     * @param loginDTO 登录参数（包含微信code）
     * @return 登录结果（包含token和用户信息）
     */
    @ApiOperation("微信小程序登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody @Validated LoginDTO loginDTO) {
        log.info("微信登录请求，code: {}", loginDTO.getCode());
        LoginVO loginVO = authService.wechatLogin(loginDTO);
        return Result.success(loginVO);
    }

    /**
     * 刷新Token
     * @param refreshToken 刷新令牌
     * @return 新的访问令牌
     */
    @ApiOperation("刷新Token")
    @PostMapping("/refresh")
    public Result<LoginVO> refreshToken(@RequestParam String refreshToken) {
        log.info("刷新Token请求");
        LoginVO loginVO = authService.refreshToken(refreshToken);
        return Result.success(loginVO);
    }

    /**
     * 退出登录
     * @param userId 用户ID（从JWT中获取）
     * @return 操作结果
     */
    @ApiOperation("退出登录")
    @PostMapping("/logout")
    public Result<Void> logout(@RequestAttribute("userId") Long userId) {
        log.info("用户退出登录，userId: {}", userId);
        authService.logout(userId);
        return Result.success();
    }

    /**
     * 获取当前用户信息
     * @param userId 用户ID（从JWT中获取）
     * @return 用户信息
     */
    @ApiOperation("获取当前用户信息")
    @GetMapping("/userinfo")
    public Result<LoginVO> getUserInfo(@RequestAttribute("userId") Long userId) {
        log.info("获取用户信息，userId: {}", userId);
        LoginVO userInfo = authService.getUserInfo(userId);
        return Result.success(userInfo);
    }
}
