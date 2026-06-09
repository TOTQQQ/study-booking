package com.studyroom.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/login",
                        "/auth/refresh",
                        "/seat/query",
                        "/reservation/check",
                        "/swagger-ui/**",
                        "/swagger-resources/**",
                        "/v2/api-docs",
                        "/v3/api-docs/**",
                        "/doc.html",
                        "/webjars/**",
                        "/actuator/**"
                );
    }
}
