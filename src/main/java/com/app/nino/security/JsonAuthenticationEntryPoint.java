package com.app.nino.security;

import com.app.nino.model.dto.response.BaseResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Thay the Http403ForbiddenEntryPoint mac dinh cua Spring Security — khi
 * request thieu JWT hoac JWT khong hop le, tra ve 401 JSON dung format
 * BaseResponse (thay vi trang loi 403 mac dinh cua servlet container).
 */
@Component
@RequiredArgsConstructor
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                          AuthenticationException authException) throws IOException {
        String errorCode = (String) request.getAttribute("authErrorCode");
        if (errorCode == null) {
            errorCode = "UNAUTHORIZED";
        }
        String message = "TOKEN_EXPIRED".equals(errorCode)
            ? "Token đã hết hạn"
            : "Bạn chưa đăng nhập hoặc token không hợp lệ";

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
            BaseResponse.error(message, errorCode)));
    }
}
