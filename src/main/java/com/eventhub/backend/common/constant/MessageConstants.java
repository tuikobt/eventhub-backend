package com.eventhub.backend.common.constant;

public class MessageConstants {
    public static final String REGISTER_SUCCESS = "Đăng ký thành công";
    public static final String LOGIN_SUCCESS = "Đăng nhập thành công";
    public static final String LOGOUT_SUCCESS = "Đăng xuất thành công";
    public static final String TOKEN_REFRESHED = "Token đã được làm mới";

    public static final String REFRESH_TOKEN_MISSING = "Refresh token không được cung cấp";
    public static final String REFRESH_TOKEN_NOT_FOUND = "Refresh token không tồn tại";
    public static final String REFRESH_TOKEN_REUSE_DETECTED = "Phát hiện refresh token bị sử dụng lại. Đã thu hồi toàn bộ session. Vui lòng đăng nhập lại.";
    public static final String REFRESH_TOKEN_EXPIRED = "Refresh token đã hết hạn. Vui lòng đăng nhập lại.";

    public static final String EMAIL_ALREADY_EXISTS = "Email đã được sử dụng";

    private MessageConstants() {
        // Prevent instantiation
    }
}
