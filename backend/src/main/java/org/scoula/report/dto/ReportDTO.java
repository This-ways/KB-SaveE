package org.scoula.report.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.scoula.peerstat.dto.CategoryCompareDTO;

import java.util.List;

// GET /api/report 전체 응답 구조 (소비 리포트 화면 하나에 필요한 데이터 총집합)
//
// [구현 상태 안내]
// - cashFlow / weeklySpending / weekdaySpending / topCategories
//   = 본인(Transaction/PeerStat) 담당, 실제 쿼리로 완성됨
// - saveAmount(GOAL, A팀) / savingPayment(PAYMENT, C팀)
//   = 아직 남의 도메인이라 임시 목업값. 나중에 각 팀 서비스 호출로 교체 예정 (필드 구조는 안 바뀜)
// - aiSummary
//   = OpenAI 연동 전까지 정적 문구로 임시 대체
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReportDTO {
    private String yearMonth;
    private CashFlow cashFlow;

    private Integer saveAmount;     // TODO: A팀 GOAL 연계 전까지 목업값
    private Integer savingPayment;  // TODO: C팀 PAYMENT 연계 전까지 목업값

    private List<WeeklyAmountDTO> weeklySpending;
    private List<WeekdayAmountDTO> weekdaySpending;
    private List<CategoryCompareDTO> topCategories; // 내 소비 vs 연령별 소비 Top3 (PeerStat 재사용)

    private String aiSummary; // TODO: OpenAI 연동 전까지 정적 문구

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CashFlow {
        private Integer prevBalance;    // 전월 잔액 (deposit_account 기준 시작잔액 + 그 이전 거래 순증감)
        private Integer income;         // 이번 달 수입
        private Integer expense;        // 이번 달 지출
        private Integer currentBalance; // 이달 잔액 (= prevBalance + income - expense)
    }
}
