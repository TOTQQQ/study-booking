package com.studyroom.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * 微信工具类
 */
@Component
@Slf4j
public class WechatUtil {

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String JSCODE2SESSION_URL = 
            "https://api.weixin.qq.com/sns/jscode2session?appid={appid}&secret={secret}&js_code={code}&grant_type=authorization_code";

    /**
     * 调用微信jscode2session接口
     * @param appId 小程序AppID
     * @param secret 小程序Secret
     * @param code 登录凭证
     * @return 微信返回结果
     */
    public JSONObject jscode2session(String appId, String secret, String code) {
        String url = JSCODE2SESSION_URL
                .replace("{appid}", appId)
                .replace("{secret}", secret)
                .replace("{code}", code);

        log.info("调用微信登录接口，code: {}", code);

        try {
            String response = restTemplate.getForObject(url, String.class);
            log.info("微信登录接口返回: {}", response);
            return JSON.parseObject(response);
        } catch (Exception e) {
            log.error("调用微信登录接口失败: {}", e.getMessage());
            JSONObject error = new JSONObject();
            error.put("errcode", -1);
            error.put("errmsg", e.getMessage());
            return error;
        }
    }
}
