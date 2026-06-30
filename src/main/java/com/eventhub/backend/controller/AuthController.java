package com.eventhub.backend.controller;

import com.eventhub.backend.dto.ApiResponse;
import com.eventhub.backend.dto.AuthResponse;
import com.eventhub.backend.dto.LoginRequest;
import com.eventhub.backend.dto.RegisterRequest;
import com.eventhub.backend.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

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
        // (AuthService đã tạo & lưu DB, ta cần lấy lại token string)
        // Lưu ý: cách đơn giản hơn ở đây là return cả refreshToken trong response
        // rồi client tự lưu — nhưng httpOnly cookie bảo mật hơn

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đăng ký thành công", authResponse));
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

        return ResponseEntity
                .ok(ApiResponse.success("Đăng nhập thành công", authResponse));
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
            refreshToken = request.getHeader("X-Refresh-Token");
        }

        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Refresh token không được cung cấp"));
        }

        AuthResponse authResponse = authService.refreshToken(refreshToken);

        return ResponseEntity
                .ok(ApiResponse.success("Token đã được làm mới", authResponse));
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
            refreshToken = request.getHeader("X-Refresh-Token");
        }

        if (refreshToken != null) {
            authService.logout(refreshToken);
        }

        // Xóa cookie
        Cookie cookie = new Cookie("refreshToken", "");
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // Đổi thành true khi deploy HTTPS
        cookie.setPath("/api/auth");
        cookie.setMaxAge(0); // Xóa cookie
        response.addCookie(cookie);

        return ResponseEntity
                .ok(ApiResponse.success("Đăng xuất thành công", null));
    }

    // ===== Helper =====

    private String extractRefreshTokenFromCookies(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> "refreshToken".equals(c.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElse(null);
    }
}
