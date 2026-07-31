package org.scoula.report.mapper;

import org.apache.ibatis.annotations.Param;
import org.scoula.report.dto.IncomeExpenseDTO;
import org.scoula.report.dto.WeekdayAmountDTO;
import org.scoula.report.dto.WeeklyAmountDTO;

import java.util.List;

public interface ReportMapper {

    // deposit_account에 세팅된 기준 시작잔액 (계좌 연결 시점의 실제 잔액)
    Integer getInitialBalance(@Param("userId") Long userId);

    // 기준 시작잔액 이후 ~ 해당 월 시작 전까지, 모든 거래의 순증감액 (수입 +, 지출 -)
    Integer getBalanceDeltaBefore(
            @Param("userId") Long userId,
            @Param("yearMonth") String yearMonth
    );

    // 이번 달 수입/지출 합계
    IncomeExpenseDTO getMonthlyIncomeExpense(
            @Param("userId") Long userId,
            @Param("yearMonth") String yearMonth
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
}
