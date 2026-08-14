package org.scoula.report.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.common.util.ClockService;
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
    private final ClockService clockService;

    @Override
    public ReportDTO getReport(Long userId, String yearMonth) {
        // 1. 이번 달 수입 / 소비성 지출(적금이체 제외) / 적금납입액 - 잔액 계산에 다 필요해서 먼저 구함
        IncomeExpenseDTO incomeExpense = mapper.getMonthlyIncomeExpense(userId, yearMonth);
        int income = incomeExpense.getIncome();
        int expense = incomeExpense.getExpense(); // 소비성 지출만 (적금이체 제외)
        int savingPayment = mapper.getMonthlyPaymentAmount(userId, yearMonth);

        // 2. 이달잔액(=조회 대상 월 말일 기준 마감잔액) 계산
        //    - 이번 달(진행 중)이면: 항상 실시간(deposit_account.balance 그대로)
        //    - 과거 달이면: 캐시에 있으면 그대로 재사용, 없으면 이번 한 번만 계산해서 캐시에 저장
        boolean isCurrentMonth = yearMonth.equals(clockService.currentYearMonth().toString());
        int currentBalance;

        if (isCurrentMonth) {
            currentBalance = mapper.getCurrentBalance(userId);
        } else {
            Integer cached = mapper.findBalanceSnapshot(userId, yearMonth);
            if (cached != null) {
                currentBalance = cached;
            } else {
                int liveBalance = mapper.getCurrentBalance(userId);
                int netAfter = mapper.getNetChangeAfterMonth(userId, yearMonth);
                currentBalance = liveBalance - netAfter;
                mapper.insertBalanceSnapshot(userId, yearMonth, currentBalance);
            }
        }

        // 3. 전월잔액 = 이달잔액 - 수입 + 지출(소비성만) + 적금납입액
        //    ("지출"엔 안 잡히지만 실제로 나간 적금납입액을 따로 더해줘야 잔액이 정확히 맞음)
        int prevBalance = currentBalance - income + expense + savingPayment;

        ReportDTO.CashFlow cashFlow = new ReportDTO.CashFlow(prevBalance, income, expense, currentBalance);

        List<WeeklyAmountDTO> weeklySpending = mapper.getWeeklySpending(userId, yearMonth);
        List<WeekdayAmountDTO> weekdaySpending = mapper.getWeekdaySpending(userId, yearMonth);

        // PeerStat 도메인 재사용 (이미 완성된 로직 그대로 갖다 씀)
        List<CategoryCompareDTO> topCategories = peerStatService.compareCategories(userId, yearMonth);

        // GOAL(A팀) 연동 - 세이브 금액 실제 계산
        Integer saveAmount = calculateSaveAmount(userId, yearMonth);

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