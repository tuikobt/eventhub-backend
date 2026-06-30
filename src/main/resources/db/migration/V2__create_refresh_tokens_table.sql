-- Bảng refresh_tokens: hỗ trợ Refresh Token Rotation + Reuse Detection
--
-- Mỗi lần gọi /refresh:
--   1. Server tạo refresh token MỚI, revoke token CŨ ngay lập tức
--   2. Nếu phát hiện token đã bị revoke được dùng lại (reuse detection)
--      → revoke TOÀN BỘ token của user đó (buộc login lại mọi thiết bị)

CREATE TABLE refresh_tokens (
    id          BIGSERIAL       PRIMARY KEY,
    user_id     BIGINT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       VARCHAR(500)    NOT NULL UNIQUE,
    revoked     BOOLEAN         NOT NULL DEFAULT FALSE,
    expires_at  TIMESTAMP       NOT NULL,
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index: tìm token nhanh khi gọi /refresh
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);

-- Index: tìm tất cả token của 1 user (dùng khi revoke toàn bộ / logout all devices)
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
