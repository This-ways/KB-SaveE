package org.scoula.report.mapper;

import org.apache.ibatis.annotations.Param;
import org.scoula.report.domain.AiSummaryVO;

public interface AiSummaryMapper {

    // 이미 생성된 요약이 있는지 확인 (없으면 null)
    AiSummaryVO find(@Param("userId") Long userId, @Param("yearMonth") String yearMonth);

    // 새로 생성한 요약 저장
    int insert(AiSummaryVO vo);
}
