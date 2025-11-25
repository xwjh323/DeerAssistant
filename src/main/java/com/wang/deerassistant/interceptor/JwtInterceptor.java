package com.wang.deerassistant.interceptor;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.wang.deerassistant.common.ApiResponse;
import com.wang.deerassistant.common.ResponseUtil;
import com.wang.deerassistant.context.UserContext;
import com.wang.deerassistant.entity.User;
import com.wang.deerassistant.mapper.UserMapper;
import com.wang.deerassistant.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) throws Exception {

        String token = req.getHeader("Authorization");

        if (token == null || !token.startsWith("Bearer ")) {
            return unauthorized(resp, "未登录");
        }

        token = token.substring(7); // 去掉 "Bearer "

        try {
            DecodedJWT jwt = jwtUtil.verify(token);
            Long userId = Long.valueOf(jwt.getSubject());
            Integer role = jwt.getClaim("role").asInt();

            // 查询用户状态
            User user = userMapper.selectById(userId);
            if (user == null || user.getStatus() != 1) {
                return unauthorized(resp, "账号已被禁用");
            }

            String path = req.getRequestURI();

            // 管理员模块校验
            if (path.startsWith("/api/admin") && role != 0) {
                return forbidden(resp, "无访问权限");
            }

            // 保存到 ThreadLocal
            UserContext.setUserId(userId);
            return true;

        } catch (Exception e) {
            return unauthorized(resp, "登录已过期或无效 token");
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest req, HttpServletResponse resp, Object handler, Exception ex) {
        // 请求结束 清理 ThreadLocal，防止内存泄露
        UserContext.clear();
    }

    private boolean unauthorized(HttpServletResponse resp, String msg) throws IOException {
        resp.setStatus(401);
        resp.setContentType("application/json;charset=UTF-8");
        ApiResponse<?> error = ResponseUtil.error(401, msg);
        resp.getWriter().write(objectMapper.writeValueAsString(error));
        return false;
    }

    private boolean forbidden(HttpServletResponse resp, String msg) throws IOException {
        resp.setStatus(403);
        resp.setContentType("application/json;charset=UTF-8");
        ApiResponse<?> error = ResponseUtil.error(403, msg);
        resp.getWriter().write(objectMapper.writeValueAsString(error));
        return false;
    }
}
