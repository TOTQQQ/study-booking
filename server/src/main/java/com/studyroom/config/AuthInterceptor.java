package com.studyroom.config;

import com.studyroom.exception.ErrorCode;
import com.studyroom.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 解析前端传入的JWT，并把用户ID放入请求上下文。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Value("${jwt.header:Authorization}")
    private String jwtHeader;

    @Value("${jwt.token-prefix:Bearer}")
    private String tokenPrefix;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authorization = request.getHeader(jwtHeader);
        String token = extractToken(authorization);
        if (token == null || !jwtUtil.validateToken(token)) {
            writeUnauthorized(response, "登录已过期，请重新登录");
            return false;
        }

        Long userId = jwtUtil.getUserIdFromToken(token);
        request.setAttribute("userId", userId);
        return true;
    }

    private String extractToken(String authorization) {
        if (authorization == null || authorization.trim().isEmpty()) {
            return null;
        }
        String prefix = tokenPrefix + " ";
        if (authorization.startsWith(prefix)) {
            return authorization.substring(prefix.length()).trim();
        }
        return authorization.trim();
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":" + ErrorCode.UNAUTHORIZED.getCode()
                + ",\"message\":\"" + message + "\"}");
    }
}
