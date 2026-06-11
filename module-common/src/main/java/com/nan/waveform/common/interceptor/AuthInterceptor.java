package com.nan.waveform.common.interceptor;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.nan.waveform.common.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
/**
 * @author nan chao
 * @since 2026/6/11 10:43
 */

@Component
public class AuthInterceptor implements HandlerInterceptor{
    @Autowired
    private JwtUtils jwtUtils;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 放行 OPTIONS 请求（前端跨域预检请求）
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 2. 从请求头中获取 Authorization
        String token = request.getHeader("Authorization");

        // 支持标准 Bearer Token 格式
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (token == null || token.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
            response.getWriter().write("Missing login token");
            return false;
        }

        try {
            // 3. 验证 token
            DecodedJWT decodedJWT = jwtUtils.verifyToken(token);

            // 4. 将解析出来的用户信息存入 request 域，方便后续 Controller 直接获取
            request.setAttribute("userId", decodedJWT.getClaim("userId").asLong());
            request.setAttribute("username", decodedJWT.getClaim("username").asString());

            return true; // 放行
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
            response.getWriter().write("Invalid or expired token");
            return false;
        }
    }
}
