package org.scoula.report.mapper;

import org.apache.ibatis.annotations.Param;
import org.scoula.report.domain.AiSummaryVO;

public interface AiSummaryMapper {

    // 이미 생성된 요약이 있는지 확인 (없으면 null)
    AiSummaryVO find(@Param("userId") Long userId, @Param("yearMonth") String yearMonth);

    // 새로 생성한 요약 저장
    int insert(AiSummaryVO vo);

    // 강제 새로고침 전, 기존 캐시 삭제
    int deleteOne(@Param("userId") Long userId, @Param("yearMonth") String yearMonth);

    // 10분 이내에 생성된 캐시가 있는지 확인 (재생성 제한용)
    boolean isRecentlyGenerated(@Param("userId") Long userId, @Param("yearMonth") String yearMonth);
}
