package org.scoula.user.mapper;

import org.apache.ibatis.annotations.Param;
import org.scoula.user.dto.CategoryAverageDTO;

import java.util.List;

/**
 * ⚠️ transaction 테이블은 원래 김의겸님(TRANSACTION) 도메인 소유입니다.
 * "카테고리별 평균 지출" 계산은 GOAL 화면(카테고리 선택/예상 절약액) 전용으로만 쓰이고
 * 다른 도메인 로직에 영향 안 주는 단순 집계 조회라, PeerStatMapper가 user 테이블을 직접 읽는 것과
 * 같은 방식으로 여기서 직접 조회합니다. (도메인 침범 최소화 목적)
 */
public interface TransactionStatMapper {
    /**
     * userId의 referenceYearMonth 이전 최근 monthsBack개월 간, 카테고리별 평균 지출(월 기준, 지출만).
     * 예: referenceYearMonth=2026-08, monthsBack=3 -> 2026-05, 06, 07월 합계를 3으로 나눈 값
     */
    List<CategoryAverageDTO> findCategoryAverages(@Param("userId") Long userId,
                                                   @Param("referenceYearMonth") String referenceYearMonth,
                                                   @Param("monthsBack") int monthsBack);
}
