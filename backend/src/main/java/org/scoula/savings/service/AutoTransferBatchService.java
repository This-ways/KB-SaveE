package org.scoula.savings.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.notification.domain.NotificationVO;
import org.scoula.notification.service.NotificationService;
import org.scoula.notification.util.NotificationMessage;
import org.scoula.savings.domain.PaymentVO;
import org.scoula.savings.domain.SubscriptionVO;
import org.scoula.savings.mapper.AccountMapper;
import org.scoula.savings.mapper.SavingsMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Log4j2
@Service
@RequiredArgsConstructor
public class AutoTransferBatchService {

    private final SavingsMapper savingsMapper;
    private final AccountMapper accountMapper;
    private final NotificationService notificationService;

    @Value("${batch.auto-transfer.enabled:false}")
    private boolean isBatchEnabled;

    // 공휴일 데이터 세팅
    private static final Set<LocalDate> HOLIDAYS = new HashSet<>();

    static {
        int year = LocalDate.now().getYear();
        HOLIDAYS.add(LocalDate.of(year, 1, 1));
        HOLIDAYS.add(LocalDate.of(year, 3, 1));
        HOLIDAYS.add(LocalDate.of(year, 5, 5));
        HOLIDAYS.add(LocalDate.of(year, 8, 15));
        HOLIDAYS.add(LocalDate.of(year, 10, 3));
        HOLIDAYS.add(LocalDate.of(year, 10, 9));
        HOLIDAYS.add(LocalDate.of(year, 12, 25));
    }

    /**
     * 오전 9시 - 오늘 자동이체 예정 알림
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void notifyUpcomingAutoTransfer() {

        if (!isBatchEnabled) {
            return;
        }

        List<SubscriptionVO> targetList = getTodayAutoTransferTargets();

        for (SubscriptionVO sub : targetList) {
            Long userId = accountMapper.selectUserIdByDepositId(sub.getDepositId());
            sendPaymentNotification(userId, "PAY_UPCOMING");
        }

        log.info("[자동이체 예정 알림] {}건 발송", targetList.size());
    }

    /**
     * 오전 10시 - 자동이체 실행
     */
    @Scheduled(cron = "0 0 10 * * *")
    public void executeDailyAutoTransfer() {

        if (!isBatchEnabled) {
            log.info("자동이체 배치가 비활성화 상태입니다.");
            return;
        }

        List<SubscriptionVO> targetList = getTodayAutoTransferTargets();

        log.info("[자동이체 배치 시작] 대상 {}건", targetList.size());

        for (SubscriptionVO sub : targetList) {

            Long userId = accountMapper.selectUserIdByDepositId(sub.getDepositId());

            try {

                processSingleTransfer(sub);

                sendPaymentNotification(userId, "PAY_SUCCESS");

                log.info("자동이체 성공 - Subscription ID: {}", sub.getSubscriptionId());

            } catch (Exception e) {

                sendPaymentNotification(userId, "PAY_FAIL");

                log.error("자동이체 실패 - Subscription ID: {}, 사유: {}",
                        sub.getSubscriptionId(), e.getMessage());
            }
        }

        log.info("[자동이체 배치 종료] 총 {}건 처리", targetList.size());
    }

    /**
     * 오늘 자동이체 대상 조회
     * (공휴일 이월 + 말일 보정 포함)
     */
    private List<SubscriptionVO> getTodayAutoTransferTargets() {

        LocalDate today = LocalDate.now();

        if (!isBusinessDay(today)) {
            log.info("[자동이체] 오늘은 비영업일이므로 대상이 없습니다.");
            return Collections.emptyList();
        }

        List<LocalDate> coveredDates = new ArrayList<>();
        coveredDates.add(today);

        LocalDate prevDay = today.minusDays(1);

        while (!isBusinessDay(prevDay)) {
            coveredDates.add(prevDay);
            prevDay = prevDay.minusDays(1);
        }

        Set<Integer> targetDaysSet = new HashSet<>();

        for (LocalDate date : coveredDates) {

            int day = date.getDayOfMonth();
            targetDaysSet.add(day);

            if (day == date.lengthOfMonth()) {
                for (int i = day + 1; i <= 31; i++) {
                    targetDaysSet.add(i);
                }
            }
        }

        List<Integer> targetDayList = new ArrayList<>(targetDaysSet);

        log.info("처리 대상 납입일 : {}", targetDayList);

        return savingsMapper.selectAutoTransferTargets(targetDayList);
    }

    @Transactional
    public void processSingleTransfer(SubscriptionVO sub) {

        Long amount = Long.valueOf(sub.getMonthlyAmount());

        Long currentBalance =
                accountMapper.selectBalanceByDepositId(sub.getDepositId());

        if (currentBalance == null || currentBalance < amount) {
            throw new RuntimeException("출금 계좌 잔액 부족");
        }

        accountMapper.withdrawBalance(sub.getDepositId(), amount);

        Integer maxRound =
                savingsMapper.selectMaxRoundNo(sub.getSubscriptionId());

        int nextRound = (maxRound != null ? maxRound : 0) + 1;

        int todayDateInt = Integer.parseInt(
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        );

        PaymentVO paymentVO = PaymentVO.builder()
                .subscriptionId(sub.getSubscriptionId())
                .roundNo(nextRound)
                .amount(amount)
                .paidAt(todayDateInt)
                .build();

        savingsMapper.insertPayment(paymentVO);
    }

    private boolean isBusinessDay(LocalDate date) {

        DayOfWeek dayOfWeek = date.getDayOfWeek();

        if (dayOfWeek == DayOfWeek.SATURDAY ||
                dayOfWeek == DayOfWeek.SUNDAY) {
            return false;
        }

        return !HOLIDAYS.contains(date);
    }

    private void sendPaymentNotification(Long userId, String typeCode) {

        NotificationVO vo = NotificationVO.builder()
                .userId(userId)
                .typeCode(typeCode)
                .build();

        notificationService.processNotification(
                vo,
                NotificationMessage.getPaymentTitle(typeCode),
                NotificationMessage.getPaymentBody(typeCode)
        );
    }
}