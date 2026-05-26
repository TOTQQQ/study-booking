package com.studyroom.service;

import com.studyroom.dto.LoginDTO;
import com.studyroom.vo.LoginVO;

/**
 * 认证服务接口
 * 负责人：严雯琪
 * 任务ID：1.3 用户微信授权登录接口开发
 */
public interface AuthService {

    /**
     * 微信小程序登录
     * @param loginDTO 登录参数
     * @return 登录结果
     */
    LoginVO wechatLogin(LoginDTO loginDTO);

    /**
     * 刷新Token
     * @param refreshToken 刷新令牌
     * @return 新的登录信息
     */
    LoginVO refreshToken(String refreshToken);

    /**
     * 退出登录
     * @param userId 用户ID
     */
    void logout(Long userId);

    /**
     * 获取用户信息
     * @param userId 用户ID
     * @return 用户信息
     */
    LoginVO getUserInfo(Long userId);
}
