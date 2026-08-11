package org.scoula.report.service;

import org.scoula.peerstat.dto.CategoryCompareDTO;

import java.util.List;

public interface AiSummaryService {

    // 캐시에 있으면 그대로, 없으면 OpenAI로 새로 생성해서 캐시에 저장 후 반환
    String getOrGenerateSummary(
            Long userId,
            String yearMonth,
            int income,
            int expense,
            List<CategoryCompareDTO> topCategories
    );

    // 캐시 무시하고 강제로 새로 생성 (10분에 1번 제한)
    String regenerateSummary(
            Long userId,
            String yearMonth,
            int income,
            int expense,
            List<CategoryCompareDTO> topCategories
    );
}
