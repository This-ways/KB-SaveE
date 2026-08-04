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

import org.scoula.notification.util.NotificationMessage;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;



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

    //테스트할때 스케줄 바꾸기
    @Scheduled(cron = "0 0 10 * * *")
    public void executeDailyAutoTransfer() {
        if (!isBatchEnabled) {
            log.info("자동이체 배치가 비활성화 상태이므로 실행하지 않습니다.");
            return;
        }

        LocalDate today = LocalDate.now();

        // 1. 오늘이 비영업일이면 이월을 위해 스킵
        if (!isBusinessDay(today)) {
            log.info("[자동이체 배치 스킵] 오늘은 비영업일({}월 {}일)입니다.", today.getMonthValue(), today.getDayOfMonth());
            return;
        }

        // 2. 오늘 및 처리하지 못한 밀린 비영업일 날짜(LocalDate) 모두 수집
        List<LocalDate> coveredDates = new ArrayList<>();
        coveredDates.add(today);

        LocalDate prevDay = today.minusDays(1);
        while (!isBusinessDay(prevDay)) {
            coveredDates.add(prevDay);
            prevDay = prevDay.minusDays(1);
        }

        // 3. 수집된 날짜들을 기반으로 납입일(payment_day) 추출 및 말일 보정
        // 중복 방지를 위해 Set 사용 (예: 31일이 여러 번 들어가는 것 방지)
        Set<Integer> targetDaysSet = new HashSet<>();

        for (LocalDate date : coveredDates) {
            int day = date.getDayOfMonth();
            targetDaysSet.add(day);

            // 말일 보정 로직: 해당 날짜가 '그 달의 마지막 날'인지 확인
            // 맞다면, 설정 불가능한 뒤의 날짜들(예: 31일 설정자)을 모두 오늘 출금 대상에 포함!
            if (day == date.lengthOfMonth()) {
                for (int i = day + 1; i <= 31; i++) {
                    targetDaysSet.add(i);
                }
            }
        }

        // Set을 List로 변환하여 MyBatis로 전달
        List<Integer> targetDayList = new ArrayList<>(targetDaysSet);
        log.info("[자동이체 배치 시작] 기준일: {}, 처리 대상 납입일(말일/이월 포함): {}", today, targetDayList);

        // 4. 대상자 목록 한 번에 조회 및 처리
        List<SubscriptionVO> targetList = savingsMapper.selectAutoTransferTargets(targetDayList);

        for (SubscriptionVO sub : targetList) {
            // depositId로 userId 조회
            Long userId = accountMapper.selectUserIdByDepositId(sub.getDepositId());

            try {
                processSingleTransfer(sub);

                // 성공 알림
                sendPaymentNotification(userId, "PAY_SUCCESS");

                log.info("자동이체 성공 - Subscription ID: {}", sub.getSubscriptionId());

            } catch (Exception e) {

                // 실패 알림
                sendPaymentNotification(userId, "PAY_FAIL");

                log.error("자동이체 실패 - Subscription ID: {}, 사유: {}", sub.getSubscriptionId(), e.getMessage());
            }
        }

        log.info("[자동이체 배치 종료] 총 {}건 처리 완료", targetList.size());
    }

    @Transactional
    public void processSingleTransfer(SubscriptionVO sub) {
        Long amount = Long.valueOf(sub.getMonthlyAmount());

        Long currentBalance = accountMapper.selectBalanceByDepositId(sub.getDepositId());
        if (currentBalance == null || currentBalance < amount) {
            throw new RuntimeException("출금 계좌 잔액 부족 (현재 잔액: " + currentBalance + ")");
        }

        accountMapper.withdrawBalance(sub.getDepositId(), amount);

        Integer maxRound = savingsMapper.selectMaxRoundNo(sub.getSubscriptionId());
        int nextRound = (maxRound != null ? maxRound : 0) + 1;

        int todayDateInt = Integer.parseInt(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));

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
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return false;
        }
        if (HOLIDAYS.contains(date)) {
            return false;
        }
        return true;
    }

    private void sendPaymentNotification(Long userId, String typeCode) {

        String title;
        String body;

        if ("PAY_SUCCESS".equals(typeCode)) {
            title = "자동이체 완료";
            body = "적금 자동이체가 정상적으로 완료되었습니다.";
        } else {
            title = "자동이체 실패";
            body = "잔액 부족으로 적금 자동이체에 실패했습니다.";
        }

        NotificationVO vo = NotificationVO.builder()
                .userId(userId)
                .typeCode(typeCode)
                .build();

        notificationService.processNotification(vo, title, body);
    }
}