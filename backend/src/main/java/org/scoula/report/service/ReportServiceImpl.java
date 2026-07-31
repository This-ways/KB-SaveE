package org.scoula.report.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.peerstat.dto.CategoryCompareDTO;
import org.scoula.peerstat.service.PeerStatService;
import org.scoula.report.dto.IncomeExpenseDTO;
import org.scoula.report.dto.ReportDTO;
import org.scoula.report.dto.WeekdayAmountDTO;
import org.scoula.report.dto.WeeklyAmountDTO;
import org.scoula.report.mapper.ReportMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Log4j2
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    // TODO: A팀 GOAL 서비스 나오면 실제 호출로 교체 (지금은 화면 확인용 목업)
    private static final int MOCK_SAVE_AMOUNT = 87_000;

    // TODO: C팀 PAYMENT 서비스 나오면 실제 호출로 교체 (지금은 화면 확인용 목업)
    private static final int MOCK_SAVING_PAYMENT = 300_000;

    // TODO: OpenAI 연동 전까지 정적 문구로 대체
    private static final String MOCK_AI_SUMMARY =
            "배달/간식 지출이 조금씩 늘었어요. 다음 달엔 조금 더 줄여보는 건 어떨까요?";

    private final ReportMapper mapper;
    private final PeerStatService peerStatService; // 다른 도메인의 Service를 그대로 주입받아 재사용

    @Override
    public ReportDTO getReport(Long userId, String yearMonth) {
        // 1. 전월잔액 = 기준 시작잔액 + 그 이전까지의 순증감액
        int initialBalance = mapper.getInitialBalance(userId);
        int deltaBefore = mapper.getBalanceDeltaBefore(userId, yearMonth);
        int prevBalance = initialBalance + deltaBefore;

        // 2. 이번 달 수입/지출
        IncomeExpenseDTO incomeExpense = mapper.getMonthlyIncomeExpense(userId, yearMonth);
        int income = incomeExpense.getIncome();
        int expense = incomeExpense.getExpense();

        // 3. 이달잔액 = 전월잔액 + 이번달 수입 - 이번달 지출
        int currentBalance = prevBalance + income - expense;

        ReportDTO.CashFlow cashFlow = new ReportDTO.CashFlow(prevBalance, income, expense, currentBalance);

        List<WeeklyAmountDTO> weeklySpending = mapper.getWeeklySpending(userId, yearMonth);
        List<WeekdayAmountDTO> weekdaySpending = mapper.getWeekdaySpending(userId, yearMonth);

        // PeerStat 도메인 재사용 (이미 완성된 로직 그대로 갖다 씀)
        List<CategoryCompareDTO> topCategories = peerStatService.compareCategories(userId, yearMonth);

        log.debug("report userId={} yearMonth={} prevBalance={} income={} expense={} currentBalance={}",
                userId, yearMonth, prevBalance, income, expense, currentBalance);

        return new ReportDTO(
                yearMonth,
                cashFlow,
                MOCK_SAVE_AMOUNT,
                MOCK_SAVING_PAYMENT,
                weeklySpending,
                weekdaySpending,
                topCategories,
                MOCK_AI_SUMMARY
        );
    }
}
