CREATE
DATABASE IF NOT EXISTS kb_savee CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE
kb_savee;


CREATE TABLE IF NOT EXISTS `category`
(
    category_id
    BIGINT
    NOT
    NULL
    AUTO_INCREMENT,
    `name`
    VARCHAR
(
    30
) NOT NULL,
    PRIMARY KEY
(
    category_id
)
    );

INSERT INTO `category` (category_id, `name`)
VALUES (1, '식비'),
       (2, '카페/간식'),
       (3, '온라인쇼핑'),
       (4, '패션/쇼핑'),
       (5, '문화/여가'),
       (6, '술/유흥'),
       (7, '교통'),
       (8, '생활'),
       (9, '뷰티'),
       (10, '교육'),
       (11, '반려동물'),
       (12, '여행'),
       (13, '기타');

-- ------------------------------------------------------------
-- USER
-- ------------------------------------------------------------
CREATE TABLE `user`
(
    user_id          BIGINT       NOT NULL AUTO_INCREMENT,
    login_id         VARCHAR(50)  NOT NULL,
    password         VARCHAR(255) NOT NULL,
    user_name        VARCHAR(50)  NOT NULL,
    birth_date       DATE         NOT NULL,
    push_enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    mydata_connected BOOLEAN      NOT NULL DEFAULT FALSE,             -- 마이데이터(계좌/카드) 연결 여부. 신규가입은 기본 미연결
    status           TINYINT      NOT NULL DEFAULT 10,                -- 10 정상 / 20 휴면 / 90 탈퇴
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 가입일 = 트라이얼/정식 판별 기준
    updated_at       DATETIME NULL,
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_user_login_id (login_id)
);

-- ------------------------------------------------------------
-- GOAL
-- ------------------------------------------------------------
CREATE TABLE `goal`
(
    goal_id       BIGINT  NOT NULL AUTO_INCREMENT,
    user_id       BIGINT  NOT NULL,
    category_id   BIGINT  NOT NULL,
    `year_month`  CHAR(7) NOT NULL, -- 'YYYY-MM' (year가 예약어라 백틱 필수)
    target_amount INT     NOT NULL,
    PRIMARY KEY (goal_id),
    UNIQUE KEY uk_goal (user_id, category_id, `year_month`),
    CONSTRAINT fk_goal_user FOREIGN KEY (user_id) REFERENCES `user` (user_id) ON DELETE RESTRICT,
    CONSTRAINT fk_goal_category FOREIGN KEY (category_id) REFERENCES `category` (category_id) ON DELETE RESTRICT
);

-- ------------------------------------------------------------
-- DEPOSIT_ACCOUNT
-- ------------------------------------------------------------
CREATE TABLE `deposit_account`
(
    deposit_id BIGINT   NOT NULL AUTO_INCREMENT,
    user_id    BIGINT   NOT NULL,
    balance    INT      NOT NULL DEFAULT 1000000,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL,
    PRIMARY KEY (deposit_id),
    UNIQUE KEY uk_deposit_user (user_id),
    CONSTRAINT fk_deposit_user FOREIGN KEY (user_id) REFERENCES `user` (user_id) ON DELETE RESTRICT
);

INSERT INTO `user` (login_id, password, user_name, birth_date, push_enabled, mydata_connected, status, created_at)
VALUES ('mj0517', '$2b$10$1Z2UaLYkTk24WQL1MLmpBeSS1jai2zh3HNpzQae9pl0m59ON4SRaG', '김민지', '2004-05-17', TRUE, TRUE, 10,
        '2026-04-05 09:00:00'),
       ('js0203', '$2b$10$1Z2UaLYkTk24WQL1MLmpBeSS1jai2zh3HNpzQae9pl0m59ON4SRaG', '박준서', '2000-02-03', TRUE, TRUE, 10,
        '2026-05-19 09:12:00'),
       ('dh0911', '$2b$10$1Z2UaLYkTk24WQL1MLmpBeSS1jai2zh3HNpzQae9pl0m59ON4SRaG', '이도현', '1998-09-11', TRUE, TRUE, 10,
        '2026-06-08 10:03:00'),
       ('sm0625', '$2b$10$1Z2UaLYkTk24WQL1MLmpBeSS1jai2zh3HNpzQae9pl0m59ON4SRaG', '최성민', '1992-06-25', TRUE, TRUE, 10,
        '2026-07-22 11:20:00'),
       ('yj0130', '$2b$10$1Z2UaLYkTk24WQL1MLmpBeSS1jai2zh3HNpzQae9pl0m59ON4SRaG', '정유진', '1995-01-30', TRUE, FALSE, 10,
        '2026-08-11 14:45:00');
-- ⚠️ 정유진만 mydata_connected=FALSE — 시연 시나리오상 "아직 데이터 연결 전" 신규 트라이얼 유저 역할
-- ⚠️ 정유진 created_at은 의도적으로 "실제 오늘(2026-07-30)보다 미래" — 8/26 발표 시연 시점엔 이미 지나있음.
--    지금 개발 중 이 유저의 정식전환 로직을 테스트하려면 mock-today 설정을 8월로 맞추고 확인할 것.

INSERT INTO `deposit_account` (user_id, balance)
VALUES (1, 500000),  -- 김민지(대학생)
       (2, 800000),  -- 박준서(대학원생)
       (3, 1200000), -- 이도현(사회초년생)
       (4, 4000000), -- 최성민(직장인)
       (5, 2500000);
-- 정유진(프리랜서)

INSERT INTO `goal` (user_id, category_id, `year_month`, target_amount)
VALUES (1, 1, '2026-04', 200000),  -- 김민지: 식비
       (1, 2, '2026-04', 60000),   -- 김민지: 카페/간식
       (2, 1, '2026-05', 200000),  -- 박준서: 식비
       (2, 8, '2026-05', 150000),  -- 박준서: 생활 (category_id 8)
       (2, 10, '2026-05', 178750), -- 박준서: 교육 (category_id 10)
       (3, 4, '2026-06', 250000),  -- 이도현: 패션/쇼핑 (category_id 4)
       (3, 6, '2026-06', 150000),  -- 이도현: 술/유흥
       (4, 7, '2026-07', 250000),  -- 최성민: 교통
       (4, 2, '2026-07', 90000),   -- 최성민: 카페/간식
       (4, 8, '2026-07', 150000);
-- 최성민: 생활 (category_id 8)

CREATE TABLE IF NOT EXISTS `transaction`
(
    txn_id
    BIGINT
    NOT
    NULL
    AUTO_INCREMENT,
    user_id
    BIGINT
    NOT
    NULL,
    category_id
    BIGINT
    NULL, -- 수입/월세이체 등은 NULL
    merchant_name
    VARCHAR
(
    100
) NULL,
    amount INT NOT NULL,
    txn_date DATE NOT NULL,
    txn_type TINYINT NOT NULL, -- 0 지출 / 1 수입
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY
(
    txn_id
),
    CONSTRAINT fk_txn_user FOREIGN KEY
(
    user_id
) REFERENCES `user`
(
    user_id
) ON DELETE RESTRICT,
    CONSTRAINT fk_txn_category FOREIGN KEY
(
    category_id
) REFERENCES `category`
(
    category_id
)
  ON DELETE RESTRICT
    );

UPDATE deposit_account da
    JOIN (SELECT user_id, SUM (CASE WHEN txn_type=1 THEN amount ELSE -amount END) AS net_change
    FROM `transaction` GROUP BY user_id) t
ON t.user_id = da.user_id
    SET da.balance = da.balance + t.net_change, da.updated_at = NOW();