package com.eventhub.backend.service;

import com.eventhub.backend.dto.AuthResponse;
import com.eventhub.backend.dto.LoginRequest;
import com.eventhub.backend.dto.RegisterRequest;
import com.eventhub.backend.entity.RefreshToken;
import com.eventhub.backend.entity.Role;
import com.eventhub.backend.entity.User;
import com.eventhub.backend.exception.EmailAlreadyExistsException;
import com.eventhub.backend.exception.TokenException;
import com.eventhub.backend.repository.RefreshTokenRepository;
import com.eventhub.backend.repository.UserRepository;
import com.eventhub.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    /**
     * ĐĂNG KÝ tài khoản mới
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // 1. Kiểm tra email đã tồn tại chưa
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(
                "Email '" + request.getEmail() + "' đã được sử dụng"
            );
        }

        // 2. Tạo User entity
        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .emailVerified(false)
                .build();

        // 3. Lưu vào database
        userRepository.save(user);

        // 4. Tạo tokens
        String accessToken = jwtService.generateAccessToken(user);
        String refreshTokenStr = createRefreshToken(user);

        return buildAuthResponse(user, accessToken);
    }

    /**
     * ĐĂNG NHẬP
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        // 1. Authenticate (tự throw BadCredentialsException nếu sai)
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // 2. Lấy user
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow();

        // 3. Tạo tokens
        String accessToken = jwtService.generateAccessToken(user);
        String refreshTokenStr = createRefreshToken(user);

        return buildAuthResponse(user, accessToken);
    }

    /**
     * LÀM MỚI ACCESS TOKEN (Refresh Token Rotation)
     *
     * Flow:
     * 1. Tìm refresh token trong DB
     * 2. Nếu đã bị revoke → REUSE DETECTION → revoke toàn bộ token của user
     * 3. Nếu hết hạn → throw
     * 4. Revoke token cũ, tạo token mới (rotation)
     */
    @Transactional
    public AuthResponse refreshToken(String refreshTokenStr) {
        // 1. Tìm token trong DB
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenStr)
                .orElseThrow(() -> new TokenException("Refresh token không tồn tại"));

        // 2. REUSE DETECTION: token đã bị revoke mà vẫn được dùng lại
        if (refreshToken.isRevoked()) {
            // Đây là dấu hiệu token bị đánh cắp!
            // Revoke TOÀN BỘ token của user → buộc đăng nhập lại
            refreshTokenRepository.revokeAllByUserId(refreshToken.getUser().getId());
            throw new TokenException("Phát hiện refresh token bị sử dụng lại. "
                    + "Đã thu hồi toàn bộ session. Vui lòng đăng nhập lại.");
        }

        // 3. Kiểm tra hết hạn
        if (refreshToken.isExpired()) {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
            throw new TokenException("Refresh token đã hết hạn. Vui lòng đăng nhập lại.");
        }

        // 4. ROTATION: revoke token cũ, tạo token mới
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        User user = refreshToken.getUser();
        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshTokenStr = createRefreshToken(user);

        return buildAuthResponse(user, newAccessToken);
    }

    /**
     * ĐĂNG XUẤT — revoke refresh token
     */
    @Transactional
    public void logout(String refreshTokenStr) {
        refreshTokenRepository.findByToken(refreshTokenStr)
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

    // ===== Helper methods =====

    /**
     * Tạo refresh token (UUID) lưu vào DB
     */
    private String createRefreshToken(User user) {
        String tokenStr = UUID.randomUUID().toString();

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(tokenStr)
                .revoked(false)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        refreshTokenRepository.save(refreshToken);
        return tokenStr;
    }

    /**
     * Lấy refresh token string mới nhất của user (để set vào cookie)
     */
    public String getLatestRefreshToken(User user) {
        // Token mới nhất vừa tạo
        return refreshTokenRepository.findByToken(
                refreshTokenRepository.findAll().stream()
                        .filter(rt -> rt.getUser().getId().equals(user.getId()) && !rt.isRevoked())
                        .reduce((first, second) -> second)
                        .map(RefreshToken::getToken)
                        .orElse("")
        ).map(RefreshToken::getToken).orElse("");
    }

    private AuthResponse buildAuthResponse(User user, String accessToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId())
                        .fullName(user.getFullName())
                        .email(user.getEmail())
                        .role(user.getRole().name())
                        .build())
                .build();
    }
}
