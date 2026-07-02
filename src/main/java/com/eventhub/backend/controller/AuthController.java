package com.eventhub.backend.controller;

import com.eventhub.backend.constant.MessageConstants;
import com.eventhub.backend.constant.SecurityConstants;
import com.eventhub.backend.dto.request.LoginRequest;
import com.eventhub.backend.dto.request.RegisterRequest;
import com.eventhub.backend.dto.response.ApiResponse;
import com.eventhub.backend.dto.response.AuthResponse;
import com.eventhub.backend.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${app.cookie.secure:true}")
    private boolean cookieSecure;

    /**
     * ĐĂNG KÝ
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response) {

        AuthResponse authResponse = authService.register(request);

        // Set refresh token vào httpOnly cookie
        setRefreshTokenCookie(response, authResponse.getRefreshToken());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(MessageConstants.REGISTER_SUCCESS, authResponse));
    }

    /**
     * ĐĂNG NHẬP
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {

        AuthResponse authResponse = authService.login(request);

        setRefreshTokenCookie(response, authResponse.getRefreshToken());

        return ResponseEntity
                .ok(ApiResponse.success(MessageConstants.LOGIN_SUCCESS, authResponse));
    }

    /**
     * LÀM MỚI TOKEN (Refresh Token Rotation)
     * POST /api/auth/refresh
     *
     * Client gửi refreshToken qua cookie hoặc body
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {

        // Lấy refresh token từ cookie
        String refreshToken = extractRefreshTokenFromCookies(request);

        // Nếu không có trong cookie, thử lấy từ header
        if (refreshToken == null) {
            refreshToken = request.getHeader(SecurityConstants.REFRESH_TOKEN_HEADER);
        }

        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(MessageConstants.REFRESH_TOKEN_MISSING));
        }

        AuthResponse authResponse = authService.refreshToken(refreshToken);

        setRefreshTokenCookie(response, authResponse.getRefreshToken());

        return ResponseEntity
                .ok(ApiResponse.success(MessageConstants.TOKEN_REFRESHED, authResponse));
    }

    /**
     * ĐĂNG XUẤT
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response) {

        String refreshToken = extractRefreshTokenFromCookies(request);

        if (refreshToken == null) {
            refreshToken = request.getHeader(SecurityConstants.REFRESH_TOKEN_HEADER);
        }

        if (refreshToken != null) {
            authService.logout(refreshToken);
        }

        // Xóa cookie
        ResponseCookie cookie = ResponseCookie.from(SecurityConstants.COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("None")
                .path(SecurityConstants.COOKIE_PATH)
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity
                .ok(ApiResponse.success(MessageConstants.LOGOUT_SUCCESS, null));
    }

    // ===== Helper =====

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from(SecurityConstants.COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("None")
                .path(SecurityConstants.COOKIE_PATH)
                .maxAge(SecurityConstants.COOKIE_MAX_AGE_SECONDS)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String extractRefreshTokenFromCookies(HttpServletRequest request) {
        if (request.getCookies() == null)
            return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> SecurityConstants.COOKIE_NAME.equals(c.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElse(null);
    }
}
