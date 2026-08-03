-- ============================================
-- 기기토큰 (FCM 발송용) - 알림 도메인
-- ============================================
CREATE TABLE `device_token` (
                                token_id   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '기기토큰 ID',
                                user_id    BIGINT       NOT NULL                COMMENT '사용자 ID',
                                fcm_token  VARCHAR(255) NOT NULL                COMMENT 'FCM 토큰',
                                created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',
                                PRIMARY KEY (token_id),
                                UNIQUE KEY uk_device_token_fcm (fcm_token),
                                KEY idx_device_token_user (user_id),
                                CONSTRAINT fk_device_token_user
                                    FOREIGN KEY (user_id) REFERENCES `user` (user_id)
                                        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='기기 토큰';