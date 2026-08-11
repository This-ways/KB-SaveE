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
import org.scoula.transaction.dto.CategoryAmountDTO;
import org.scoula.transaction.mapper.TransactionMapper;
import org.scoula.user.dto.GoalResponseDTO;
import org.scoula.user.service.GoalService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Log4j2
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportMapper mapper;
    private final PeerStatService peerStatService;
    private final GoalService goalService;
    private final TransactionMapper transactionMapper;
    private final AiSummaryService aiSummaryService;

    @Override
    public ReportDTO getReport(Long userId, String yearMonth) {
        // 1. 전월잔액 = 기준 시작잔액(deposit_account) + 그 이전까지의 순증감액
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

        // GOAL(A팀) 연동 - 세이브 금액 실제 계산
        Integer saveAmount = calculateSaveAmount(userId, yearMonth);

        // SUBSCRIPTION/PAYMENT(C팀 담당 테이블, Service는 아직 없어서 테이블 직접 조회) - 이번 달 납입액 실제 계산
        int savingPayment = mapper.getMonthlyPaymentAmount(userId, yearMonth);

        // AI 요약 - 캐시 확인 후 없으면 OpenAI로 생성 (더 이상 목업 아님)
        String aiSummary = aiSummaryService.getOrGenerateSummary(userId, yearMonth, income, expense, topCategories);

        log.debug("report userId={} yearMonth={} prevBalance={} income={} expense={} currentBalance={} saveAmount={} savingPayment={}",
                userId, yearMonth, prevBalance, income, expense, currentBalance, saveAmount, savingPayment);

        return new ReportDTO(
                yearMonth,
                cashFlow,
                saveAmount,
                savingPayment,
                weeklySpending,
                weekdaySpending,
                topCategories,
                aiSummary
        );
    }

    @Override
    public String refreshAiSummary(Long userId, String yearMonth) {
        // AI 요약에 쓰이는 재료(수입/지출/연령별비교)만 다시 계산해서 강제 재생성에 넘김
        IncomeExpenseDTO incomeExpense = mapper.getMonthlyIncomeExpense(userId, yearMonth);
        List<CategoryCompareDTO> topCategories = peerStatService.compareCategories(userId, yearMonth);

        return aiSummaryService.regenerateSummary(
                userId, yearMonth, incomeExpense.getIncome(), incomeExpense.getExpense(), topCategories
        );
    }

    // 세이브 금액 = Σ (목표금액 - 실제지출), 카테고리별 초과 지출은 마이너스로 그대로 반영
    // 이번 달 목표가 하나도 없으면 null (프론트에서 "-"로 표시)
    private Integer calculateSaveAmount(Long userId, String yearMonth) {
        List<GoalResponseDTO> goals = goalService.findMine(userId, yearMonth);
        if (goals.isEmpty()) {
            return null;
        }

        List<CategoryAmountDTO> categorySpend = transactionMapper.getCategorySummary(userId, yearMonth);
        Map<Long, Integer> spendByCategory = new HashMap<>();
        for (CategoryAmountDTO c : categorySpend) {
            spendByCategory.put(c.getCategoryId(), c.getAmount());
        }

        int totalSave = 0;
        for (GoalResponseDTO goal : goals) {
            int actual = spendByCategory.getOrDefault(goal.getCategoryId(), 0);
            totalSave += (goal.getTargetAmount() - actual); // 초과 지출도 마이너스 그대로 합산
        }
        return totalSave;
    }
}