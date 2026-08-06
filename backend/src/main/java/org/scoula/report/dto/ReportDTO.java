package org.scoula.report.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.scoula.peerstat.dto.CategoryCompareDTO;

import java.util.List;

// GET /api/report 전체 응답 구조 (소비 리포트 화면 하나에 필요한 데이터 총집합)

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReportDTO {
    private String yearMonth;
    private CashFlow cashFlow;

    private Integer saveAmount;
    private Integer savingPayment;

    private List<WeeklyAmountDTO> weeklySpending;
    private List<WeekdayAmountDTO> weekdaySpending;
    private List<CategoryCompareDTO> topCategories; // 내 소비 vs 연령별 소비 Top3 (PeerStat 재사용)

    private String aiSummary;

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
