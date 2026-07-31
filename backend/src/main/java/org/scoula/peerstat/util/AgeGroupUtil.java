package org.scoula.peerstat.util;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeParseException;

/**
 * 생년월일(yyyy-MM-dd) -> 연령대 문자열 계산.
 * peer_stat 연령대 구간(5구간)과 동일하게 맞춤:
 *   30세 미만 -> "20대이하" (10대+20대 통합)
 *   30~39세   -> "30대"
 *   40~49세   -> "40대"
 *   50~59세   -> "50대"
 *   60세 이상  -> "60대이상"
 */
public final class AgeGroupUtil {

    private static final int SENIOR_AGE = 60;
    private static final int YOUNG_CUTOFF = 30; // 이 나이 미만은 전부 "20대이하"
    private static final String SENIOR_LABEL = "60대이상";
    private static final String YOUNG_LABEL = "20대이하";

    private AgeGroupUtil() {
    }

    public static String resolve(String birthDate) {
        if (birthDate == null || birthDate.isBlank()) {
            throw new IllegalArgumentException("birthDate가 없습니다. (해당 userId가 존재하지 않거나 user 데이터가 비어있음)");
        }

        LocalDate birth;
        try {
            birth = LocalDate.parse(birthDate);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("birthDate 형식이 올바르지 않습니다: " + birthDate, e);
        }

        int age = Period.between(birth, LocalDate.now()).getYears();

        if (age >= SENIOR_AGE) {
            return SENIOR_LABEL;
        }
        if (age < YOUNG_CUTOFF) {
            return YOUNG_LABEL;
        }
        int decade = (age / 10) * 10;
        return decade + "대";
    }
}
