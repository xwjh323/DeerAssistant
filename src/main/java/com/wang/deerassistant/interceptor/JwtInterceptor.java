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

        // ★★ 关键：放行所有跨域预检请求 OPTIONS ★★
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            resp.setStatus(HttpServletResponse.SC_OK);
            return false; // 不继续进入后续逻辑
        }

        String token = req.getHeader("Authorization");

        if (token == null || !token.startsWith("Bearer ")) {
            return writeError(resp, 401, "未登录");
        }

        token = token.substring(7); // 去掉 "Bearer "

        try {
            DecodedJWT jwt = jwtUtil.verify(token);
            Long userId = Long.valueOf(jwt.getSubject());

            // ① 查询用户信息
            User user = userMapper.selectById(userId);
            if (user == null) {
                return writeError(resp, 401, "用户不存在或已被删除");
            }

            // ② 校验用户状态（1=启用, 0=禁用）
            Integer status = user.getStatus();
            if (status != null && status == 0) {
                return writeError(resp, 403, "账号已被禁用，请联系管理员");
            }

            // ③ 判断是否访问 admin 接口
            String uri = req.getRequestURI();
            boolean adminPath = uri.startsWith("/api/admin/");

            Integer role = user.getRole(); // 约定：0=管理员，1=普通用户
            if (adminPath) {
                if (role == null || role != 0) {
                    return writeError(resp, 403, "无权访问管理员接口");
                }
            }

            // ④ 保存到 ThreadLocal
            UserContext.setUserId(userId);
            UserContext.setUserRole(role);

            return true;

        } catch (Exception e) {
            return writeError(resp, 401, "登录已过期或无效 token");
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest req, HttpServletResponse resp, Object handler, Exception ex) {
        // 请求结束 清理 ThreadLocal，防止内存泄露
        UserContext.clear();
    }

    private boolean writeError(HttpServletResponse resp, int code, String msg) throws IOException {
        resp.setStatus(code);
        resp.setContentType("application/json;charset=UTF-8");
        ApiResponse<?> error = ResponseUtil.error(code, msg);
        resp.getWriter().write(objectMapper.writeValueAsString(error));
        return false;
    }
}
