-- ============================================================
-- SaveE - User 도메인 (USER / GOAL / DEPOSIT_ACCOUNT) DDL
-- 담당: 강두형
-- CATEGORY는 김의겸님 담당 테이블의 "플레이스홀더" 버전입니다.
-- 실제 확정 DDL 받으면 이 파일의 CATEGORY 부분만 교체하면 됩니다.
-- ============================================================

CREATE DATABASE IF NOT EXISTS kb_savee CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE kb_savee;

-- ------------------------------------------------------------
-- CATEGORY - 김의겸님이 준 transaction_dummy_data.sql 기준으로 확정된 13개 category_id.
-- 이름은 가맹점 패턴으로 추정한 것이라 확정 필요 시 교체.
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `category` (
  category_id BIGINT NOT NULL AUTO_INCREMENT,
  category_name VARCHAR(50) NOT NULL,
  PRIMARY KEY (category_id)
);

INSERT INTO `category` (category_id, category_name) VALUES
  (1, '식비'),
  (2, '카페'),
  (3, '온라인쇼핑'),
  (4, '패션/의류'),
  (5, '문화/구독'),
  (6, '술/유흥'),
  (7, '교통/차량'),
  (8, '생활'),
  (9, '뷰티'),
  (10, '자기계발/구독서비스'),
  (11, '반려동물'),
  (12, '여행'),
  (13, '기타');

-- ------------------------------------------------------------
-- USER
-- ------------------------------------------------------------
CREATE TABLE `user` (
  user_id BIGINT NOT NULL AUTO_INCREMENT,
  login_id VARCHAR(50) NOT NULL,
  password VARCHAR(255) NOT NULL,
  user_name VARCHAR(50) NOT NULL,
  birth_date DATE NOT NULL,
  push_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  status TINYINT NOT NULL DEFAULT 10,      -- 10 정상 / 20 휴면 / 90 탈퇴
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,   -- 가입일 = 트라이얼/정식 판별 기준
  updated_at DATETIME NULL,
  PRIMARY KEY (user_id),
  UNIQUE KEY uk_user_login_id (login_id)
);

-- ------------------------------------------------------------
-- GOAL
-- ------------------------------------------------------------
CREATE TABLE `goal` (
  goal_id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  category_id BIGINT NOT NULL,
  `year_month` CHAR(7) NOT NULL,   -- 'YYYY-MM' (year가 예약어라 백틱 필수)
  target_amount INT NOT NULL,
  PRIMARY KEY (goal_id),
  UNIQUE KEY uk_goal (user_id, category_id, `year_month`),
  CONSTRAINT fk_goal_user FOREIGN KEY (user_id) REFERENCES `user` (user_id) ON DELETE RESTRICT,
  CONSTRAINT fk_goal_category FOREIGN KEY (category_id) REFERENCES `category` (category_id) ON DELETE RESTRICT
);

-- ------------------------------------------------------------
-- DEPOSIT_ACCOUNT
-- ------------------------------------------------------------
CREATE TABLE `deposit_account` (
  deposit_id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  balance INT NOT NULL DEFAULT 1000000,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NULL,
  PRIMARY KEY (deposit_id),
  UNIQUE KEY uk_deposit_user (user_id),
  CONSTRAINT fk_deposit_user FOREIGN KEY (user_id) REFERENCES `user` (user_id) ON DELETE RESTRICT
);

-- ------------------------------------------------------------
-- 더미데이터 - 5인 페르소나
-- 비밀번호는 전부 평문 "1234"를 실제 BCrypt로 인코딩한 값 (5명 모두 동일 해시, 테스트 로그인용)
-- ------------------------------------------------------------
INSERT INTO `user` (login_id, password, user_name, birth_date, push_enabled, status, created_at) VALUES
  ('minji',   '$2b$10$kxNYWwZopGUCtLF/ntpmJeOO6TVbVY4hKZA.Ut1kcuBnn/VbcHm0m', '김민지', '2003-03-14', TRUE, 10, '2026-04-05 10:00:00'),
  ('junseo',  '$2b$10$kxNYWwZopGUCtLF/ntpmJeOO6TVbVY4hKZA.Ut1kcuBnn/VbcHm0m', '박준서', '1999-07-02', TRUE, 10, '2026-05-11 10:00:00'),
  ('dohyeon', '$2b$10$kxNYWwZopGUCtLF/ntpmJeOO6TVbVY4hKZA.Ut1kcuBnn/VbcHm0m', '이도현', '1996-11-23', TRUE, 10, '2026-06-03 10:00:00'),
  ('seongmin','$2b$10$kxNYWwZopGUCtLF/ntpmJeOO6TVbVY4hKZA.Ut1kcuBnn/VbcHm0m', '최성민', '1993-02-08', TRUE, 10, '2026-07-01 10:00:00'),
  ('yujin',   '$2b$10$kxNYWwZopGUCtLF/ntpmJeOO6TVbVY4hKZA.Ut1kcuBnn/VbcHm0m', '정유진', '1995-09-30', TRUE, 10, '2026-08-11 10:00:00');
-- ⚠️ 정유진 created_at은 의도적으로 "실제 오늘(2026-07-30)보다 미래" — 8/26 발표 시연 시점엔 이미 지나있음.
--    지금 개발 중 이 유저의 정식전환 로직을 테스트하려면 mock-today 설정을 8월로 맞추고 확인할 것.

INSERT INTO `deposit_account` (user_id, balance) VALUES
  (1, 500000),   -- 김민지(대학생)
  (2, 800000),   -- 박준서(대학원생)
  (3, 1200000),  -- 이도현(사회초년생)
  (4, 4000000),  -- 최성민(직장인)
  (5, 2500000);  -- 정유진(프리랜서)

-- GOAL: 확정된 페르소나별 목표 (트라이얼 유저 정유진은 GOAL 없음 - 의도된 상태)
-- ⚠️ category_id는 실제 TRANSACTION 더미데이터(김의겸님 제공) 기준으로 수정됨
INSERT INTO `goal` (user_id, category_id, `year_month`, target_amount) VALUES
  (1, 1, '2026-04', 200000),   -- 김민지: 식비
  (1, 2, '2026-04', 60000),    -- 김민지: 카페
  (2, 1, '2026-05', 200000),   -- 박준서: 식비
  (2, 8, '2026-05', 150000),   -- 박준서: 생활 (기존 "생활" 의도 반영, category_id 3→8로 수정)
  (2, 10, '2026-05', 178750),  -- 박준서: 자기계발/구독 (기존 "교육" 의도 반영, category_id 4→10으로 수정)
  (3, 4, '2026-06', 250000),   -- 이도현: 패션/의류 (기존 "패션쇼핑" 의도 반영, category_id 5→4로 수정)
  (3, 6, '2026-06', 150000),   -- 이도현: 술/유흥
  (4, 7, '2026-07', 250000),   -- 최성민: 교통/차량
  (4, 2, '2026-07', 90000),    -- 최성민: 카페
  (4, 8, '2026-07', 150000);   -- 최성민: 생활 (category_id 3→8로 수정)

-- ============================================================
-- [PLACEHOLDER] TRANSACTION - 김의겸님 담당 테이블.
-- transaction_dummy_data.sql(김의겸님 제공)을 그대로 적재하기 위한 최소 컬럼 구성.
-- 확정 DDL 받으면 이 테이블 정의만 교체할 것 (MERCHANT 분리 여부 등).
-- ============================================================
CREATE TABLE IF NOT EXISTS `transaction` (
  txn_id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  category_id BIGINT NULL,          -- 수입/월세이체 등은 NULL
  merchant_name VARCHAR(100) NULL,
  amount INT NOT NULL,
  txn_date DATE NOT NULL,
  txn_type TINYINT NOT NULL,        -- 0 지출 / 1 수입
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (txn_id),
  CONSTRAINT fk_txn_user FOREIGN KEY (user_id) REFERENCES `user` (user_id) ON DELETE RESTRICT,
  CONSTRAINT fk_txn_category FOREIGN KEY (category_id) REFERENCES `category` (category_id) ON DELETE RESTRICT
);

-- 적재 순서: 위 CREATE 문들 실행 → transaction_dummy_data.sql 실행(INSERT 4,949건) → 아래 UPDATE 실행
-- source transaction_dummy_data.sql;

-- ------------------------------------------------------------
-- DEPOSIT_ACCOUNT 잔액에 1~8월 실거래 순증감 반영
-- 검증 결과: 5명 전원 플러스 잔액 (음수 유저 없음 → 초기값 재조정 불필요, 확인 완료)
-- ------------------------------------------------------------
UPDATE deposit_account da
JOIN (SELECT user_id, SUM(CASE WHEN txn_type=1 THEN amount ELSE -amount END) AS net_change
      FROM `transaction` GROUP BY user_id) t ON t.user_id = da.user_id
SET da.balance = da.balance + t.net_change, da.updated_at = NOW();

-- 확인용: 마이너스 잔액 유저 조회 (0건이어야 정상)
-- SELECT u.user_name, da.balance FROM deposit_account da JOIN `user` u ON u.user_id = da.user_id WHERE da.balance < 0;
