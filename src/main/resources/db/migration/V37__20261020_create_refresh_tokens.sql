-- ============================================================
-- V37__20261020_create_refresh_tokens.sql
-- Bang refresh_tokens: Quan ly vong doi Refresh Token bao mat
-- Ho tro: Hash SHA-256, Rotation, Reuse Detection, Sliding Expiration (tran 90 ngay)
-- ============================================================

CREATE TABLE IF NOT EXISTS `refresh_tokens` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`          BIGINT       NOT NULL,
    `token_hash`       VARCHAR(64)  NOT NULL COMMENT 'SHA-256 hash cua refresh token',
    `session_id`       VARCHAR(36)  NOT NULL COMMENT 'Claim sid cua JWT phien dang nhap nay',
    `device_id`        VARCHAR(100) DEFAULT NULL COMMENT 'Dinh danh duy nhat cua thiet bi client',
    `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `initial_login_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Thoi diem bat dau phien (dung de tinh tran tuyet doi 90 ngay)',
    `expires_at`       DATETIME     NOT NULL COMMENT 'Han refresh token hien tai (sliding 30 ngay)',
    `max_expires_at`   DATETIME     NOT NULL COMMENT 'Han toi da tuyet doi (initial_login_at + 90 ngay)',
    `revoked_at`       DATETIME     DEFAULT NULL COMMENT 'Thoi diem token bi vo hieu hoa hoac thu hoi',
    `replaced_by_id`   BIGINT       DEFAULT NULL COMMENT 'ID cua token moi da thay the token nay khi rotate',
    PRIMARY KEY (`id`),
    UNIQUE KEY `idx_refresh_tokens_token_hash` (`token_hash`),
    KEY `idx_refresh_tokens_session_id` (`session_id`),
    KEY `idx_refresh_tokens_user_id` (`user_id`),
    KEY `idx_refresh_tokens_user_device` (`user_id`, `device_id`),
    CONSTRAINT `fk_refresh_tokens_user`
        FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_refresh_tokens_replaced_by`
        FOREIGN KEY (`replaced_by_id`) REFERENCES `refresh_tokens` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
