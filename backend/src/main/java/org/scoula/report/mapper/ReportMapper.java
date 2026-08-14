package org.scoula.report.mapper;

import org.apache.ibatis.annotations.Param;
import org.scoula.report.dto.IncomeExpenseDTO;
import org.scoula.report.dto.WeekdayAmountDTO;
import org.scoula.report.dto.WeeklyAmountDTO;

import java.util.List;

public interface ReportMapper {

    // 실시간 동기화된 현재 잔액 (트리거가 항상 최신 상태로 유지 - deposit_account.balance 그대로)
    Integer getCurrentBalance(@Param("userId") Long userId);

    // 조회 대상 월 말일 이후 ~ 지금까지의 순증감 (과거 달 조회 시, 실시간잔액을 그 시점으로 되돌리기 위함)
    Integer getNetChangeAfterMonth(
            @Param("userId") Long userId,
            @Param("yearMonth") String yearMonth
    );

    // 이번 달 수입 / 소비성 지출(적금이체 제외) 합계
    IncomeExpenseDTO getMonthlyIncomeExpense(
            @Param("userId") Long userId,
            @Param("yearMonth") String yearMonth
    );

    // 과거 달 마감잔액 캐시 조회 (없으면 null)
    Integer findBalanceSnapshot(@Param("userId") Long userId, @Param("yearMonth") String yearMonth);

    // 과거 달 마감잔액 캐시 저장
    int insertBalanceSnapshot(
            @Param("userId") Long userId,
            @Param("yearMonth") String yearMonth,
            @Param("closingBalance") int closingBalance
    );

    // 주차별 소비 (지출만, 일요일 기준 달력 주차)
    List<WeeklyAmountDTO> getWeeklySpending(
            @Param("userId") Long userId,
            @Param("yearMonth") String yearMonth
    );

    // 요일별 소비 패턴 (지출만, 월~일 순 정렬)
    List<WeekdayAmountDTO> getWeekdaySpending(
            @Param("userId") Long userId,
            @Param("yearMonth") String yearMonth
    );

    // 이번 달 납입한 적금 금액 합계 (해지 여부 무관, 실제 납입된 것 전부 합산)
    Integer getMonthlyPaymentAmount(
            @Param("userId") Long userId,
            @Param("yearMonth") String yearMonth
    );
}