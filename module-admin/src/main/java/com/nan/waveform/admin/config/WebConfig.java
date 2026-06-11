package com.nan.waveform.admin.config;
import com.nan.waveform.common.interceptor.AuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
/**
 * @author nan chao
 * @since 2026/6/11 10:45
 */

@Configuration
public class WebConfig implements WebMvcConfigurer{
    @Autowired
    private AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**") // 拦截所有接口
                .excludePathPatterns("/api/auth/login", "/doc.html", "/webjars/**", "/v3/api-docs/**"); // 放行登录接口和 Knife4j 接口文档
    }
}
