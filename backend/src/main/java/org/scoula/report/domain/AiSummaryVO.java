package org.scoula.report.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// report_ai_summary 테이블 1행 (유저+월 조합당 최대 1건, 한 번 생성하면 캐싱해서 재사용)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiSummaryVO {
    private Long summaryId;
    private Long userId;
    private String yearMonth;
    private String summaryText;
    private String createdAt;
}
