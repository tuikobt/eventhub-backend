package com.eventhub.backend.common.constant;

public class SecurityConstants {
    public static final String COOKIE_NAME = "refreshToken";
    public static final String COOKIE_PATH = "/api/auth";
    public static final long COOKIE_MAX_AGE_SECONDS = 7 * 24 * 60 * 60; // 7 days in seconds
    public static final String REFRESH_TOKEN_HEADER = "X-Refresh-Token";
    public static final String TOKEN_TYPE = "Bearer";

    private SecurityConstants() {
        // Prevent instantiation
    }
}
