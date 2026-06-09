package com.studyroom.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.studyroom.constant.RedisConstant;
import com.studyroom.dto.LoginDTO;
import com.studyroom.entity.User;
import com.studyroom.exception.BusinessException;
import com.studyroom.exception.ErrorCode;
import com.studyroom.mapper.UserMapper;
import com.studyroom.service.AuthService;
import com.studyroom.utils.JwtUtil;
import com.studyroom.utils.RedisUtil;
import com.studyroom.utils.WechatUtil;
import com.studyroom.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * 认证服务实现类
 * 负责人：严雯琪
 * 任务ID：1.3 用户微信授权登录接口开发
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;
    private final WechatUtil wechatUtil;

    @Value("${wechat.miniapp.appid}")
    private String appId;

    @Value("${wechat.miniapp.secret}")
    private String appSecret;

    @Value("${wechat.miniapp.mock-login:false}")
    private Boolean mockLogin;

    @Value("${jwt.access-token-expire}")
    private Long accessTokenExpire;

    @Value("${jwt.refresh-token-expire}")
    private Long refreshTokenExpire;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO wechatLogin(LoginDTO loginDTO) {
        // 1. 调用微信接口获取openid
        String openid;
        if (Boolean.TRUE.equals(mockLogin)) {
            openid = "dev_mock_openid";
            log.info("开发环境模拟微信登录，openid: {}", openid);
        } else {
            JSONObject wechatResult = wechatUtil.jscode2session(appId, appSecret, loginDTO.getCode());

            if (wechatResult.containsKey("errcode") && wechatResult.getIntValue("errcode") != 0) {
                log.error("微信登录失败，errcode: {}, errmsg: {}",
                    wechatResult.getIntValue("errcode"),
                    wechatResult.getString("errmsg"));
                throw new BusinessException(ErrorCode.WECHAT_LOGIN_FAILED, "微信登录失败");
            }

            openid = wechatResult.getString("openid");
        }

        log.info("微信登录成功，openid: {}", openid);

        // 2. 查询或创建用户
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getOpenid, openid);
        User user = userMapper.selectOne(queryWrapper);

        boolean isNewUser = false;
        long now = System.currentTimeMillis();
        
        if (user == null) {
            // 新用户，创建记录
            user = new User();
            user.setOpenid(openid);
            user.setNickname(loginDTO.getNickname());
            user.setAvatar(loginDTO.getAvatar());
            user.setStatus(1);
            user.setCreateTime(now);
            user.setUpdateTime(now);
            userMapper.insert(user);
            isNewUser = true;
            log.info("创建新用户，userId: {}", user.getId());
        } else {
            // 老用户，更新头像昵称
            if (loginDTO.getNickname() != null) {
                user.setNickname(loginDTO.getNickname());
            }
            if (loginDTO.getAvatar() != null) {
                user.setAvatar(loginDTO.getAvatar());
            }
            user.setUpdateTime(now);
            userMapper.updateById(user);
            log.info("更新用户信息，userId: {}", user.getId());
        }

        // 3. 生成JWT令牌
        String accessToken = jwtUtil.generateAccessToken(user.getId().longValue(), openid);
        String refreshToken = jwtUtil.generateRefreshToken(user.getId().longValue(), openid);

        // 4. 缓存用户信息和token
        String userKey = RedisConstant.USER_INFO_KEY + user.getId();
        redisUtil.set(userKey, JSON.toJSONString(user), accessTokenExpire, TimeUnit.MILLISECONDS);

        String tokenKey = RedisConstant.USER_TOKEN_KEY + user.getId();
        redisUtil.set(tokenKey, accessToken, accessTokenExpire, TimeUnit.MILLISECONDS);

        String refreshTokenKey = RedisConstant.USER_REFRESH_TOKEN_KEY + user.getId();
        redisUtil.set(refreshTokenKey, refreshToken, refreshTokenExpire, TimeUnit.MILLISECONDS);

        // 5. 构建返回结果
        LoginVO loginVO = new LoginVO();
        loginVO.setUserId(user.getId().longValue());
        loginVO.setOpenid(openid);
        loginVO.setNickname(user.getNickname());
        loginVO.setAvatar(user.getAvatar());
        loginVO.setAccessToken(accessToken);
        loginVO.setRefreshToken(refreshToken);
        loginVO.setExpiresIn(accessTokenExpire / 1000);
        loginVO.setIsNewUser(isNewUser);

        return loginVO;
    }

    @Override
    public LoginVO refreshToken(String refreshToken) {
        // 1. 验证refreshToken
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "刷新令牌无效");
        }

        Long userId = jwtUtil.getUserIdFromToken(refreshToken);
        String openid = jwtUtil.getOpenidFromToken(refreshToken);

        // 2. 检查refreshToken是否在缓存中
        String refreshTokenKey = RedisConstant.USER_REFRESH_TOKEN_KEY + userId;
        String cachedRefreshToken = redisUtil.get(refreshTokenKey);
        
        if (cachedRefreshToken == null || !cachedRefreshToken.equals(refreshToken)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "刷新令牌已过期");
        }

        // 3. 生成新的accessToken
        String newAccessToken = jwtUtil.generateAccessToken(userId, openid);
        String newRefreshToken = jwtUtil.generateRefreshToken(userId, openid);

        // 4. 更新缓存
        String tokenKey = RedisConstant.USER_TOKEN_KEY + userId;
        redisUtil.set(tokenKey, newAccessToken, accessTokenExpire, TimeUnit.MILLISECONDS);
        redisUtil.set(refreshTokenKey, newRefreshToken, refreshTokenExpire, TimeUnit.MILLISECONDS);

        // 5. 获取用户信息
        User user = userMapper.selectById(userId.intValue());

        // 6. 构建返回结果
        LoginVO loginVO = new LoginVO();
        loginVO.setUserId(userId);
        loginVO.setOpenid(openid);
        loginVO.setNickname(user.getNickname());
        loginVO.setAvatar(user.getAvatar());
        loginVO.setAccessToken(newAccessToken);
        loginVO.setRefreshToken(newRefreshToken);
        loginVO.setExpiresIn(accessTokenExpire / 1000);
        loginVO.setIsNewUser(false);

        return loginVO;
    }

    @Override
    public void logout(Long userId) {
        // 清除用户缓存
        String userKey = RedisConstant.USER_INFO_KEY + userId;
        String tokenKey = RedisConstant.USER_TOKEN_KEY + userId;
        String refreshTokenKey = RedisConstant.USER_REFRESH_TOKEN_KEY + userId;

        redisUtil.delete(userKey);
        redisUtil.delete(tokenKey);
        redisUtil.delete(refreshTokenKey);

        log.info("用户退出登录，清除缓存，userId: {}", userId);
    }

    @Override
    public LoginVO getUserInfo(Long userId) {
        // 先从缓存获取
        String userKey = RedisConstant.USER_INFO_KEY + userId;
        String userJson = redisUtil.get(userKey);

        User user;
        if (userJson != null) {
            user = JSON.parseObject(userJson, User.class);
        } else {
            user = userMapper.selectById(userId.intValue());
            if (user == null) {
                throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在");
            }
            // 重新缓存
            redisUtil.set(userKey, JSON.toJSONString(user), accessTokenExpire, TimeUnit.MILLISECONDS);
        }

        LoginVO loginVO = new LoginVO();
        loginVO.setUserId(user.getId().longValue());
        loginVO.setOpenid(user.getOpenid());
        loginVO.setNickname(user.getNickname());
        loginVO.setAvatar(user.getAvatar());
        loginVO.setIsNewUser(false);

        return loginVO;
    }
}
