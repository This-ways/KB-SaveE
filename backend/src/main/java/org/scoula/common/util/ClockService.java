package org.scoula.common.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * "오늘"을 가져오는 단일 지점.
 * 실제 배포/발표 시에는 app.mock-today를 비워두면 LocalDate.now()를 그대로 사용.
 * 개발 중 트라이얼/정식전환 로직을 미리 테스트하고 싶을 때는
 * application.properties에 app.mock-today=2026-08-15 같은 값을 넣으면 그 날짜를 "오늘"로 취급한다.
 * (정유진 페르소나의 8/11 가입일처럼 실제 서버 시각보다 미래인 더미데이터를 미리 검증할 때 사용)
 */
@Component
public class ClockService {

    @Value("${app.mock-today:}")
    private String mockToday;

    public LocalDate today() {
        if (mockToday == null || mockToday.isBlank()) {
            return LocalDate.now();
        }
        return LocalDate.parse(mockToday.trim());
    }

    public YearMonth currentYearMonth() {
        return YearMonth.from(today());
    }

    /** 이번 달 카테고리 목표는 1~7일에만 수정 가능 */
    public boolean isWithinGoalEditWindow() {
        return today().getDayOfMonth() <= 30;
    }
}
