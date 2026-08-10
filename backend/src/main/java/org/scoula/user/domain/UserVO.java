package org.scoula.user.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserVO {
    private Long userId;
    private String loginId;
    private String password;
    private String userName;
    private LocalDate birthDate;
    private boolean pushEnabled;
    private boolean mydataConnected;  // 마이데이터(계좌/카드) 연결 여부 - 시연용 플래그
    private int status;              // 10 정상 / 20 휴면 / 90 탈퇴
    private int tokenVersion;        // 로그아웃 시 +1. JWT 발급 시점 버전과 비교해 무효화 처리
    private LocalDateTime createdAt; // 가입일 = 트라이얼/정식 판별 기준
    private LocalDateTime updatedAt;

    /** 가입월 (yyyy-MM) */
    public YearMonth getJoinYearMonth() {
        return YearMonth.from(createdAt.toLocalDate());
    }
}