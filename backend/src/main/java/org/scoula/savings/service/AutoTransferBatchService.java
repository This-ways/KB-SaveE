package org.scoula.savings.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.notification.domain.NotificationVO;
import org.scoula.notification.service.NotificationService;
import org.scoula.notification.util.NotificationMessage;
import org.scoula.savings.domain.SubscriptionVO;
import org.scoula.savings.mapper.AccountMapper;
import org.scoula.savings.mapper.SavingsMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

@Log4j2
@Service
@RequiredArgsConstructor
public class AutoTransferBatchService {

    private final AutoTransferProcessor transferProcessor;

    private final SavingsMapper savingsMapper;
    private final AccountMapper accountMapper;
    private final NotificationService notificationService;

    @Value("${batch.auto-transfer.enabled:false}")
    private boolean isBatchEnabled;

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

    @Scheduled(cron = "0 0 9 * * *")
    public void notifyUpcomingAutoTransfer() {
        if (!isBatchEnabled) return;
        List<SubscriptionVO> targetList = getTodayAutoTransferTargets();

        for (SubscriptionVO sub : targetList) {
            Long userId = accountMapper.selectUserIdByDepositId(sub.getDepositId());
            sendPaymentNotification(userId, "PAY_UPCOMING");
        }
        log.info("[자동이체 예정 알림] {}건 발송", targetList.size());
    }

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
                // 외부 클래스의 메서드를 호출
                transferProcessor.processSingleTransfer(sub);
                log.info("자동이체 성공 - Subscription ID: {}", sub.getSubscriptionId());

                try {
                    sendPaymentNotification(userId, "PAY_SUCCESS");
                } catch (Exception ex) {
                    log.warn("성공 알림 발송 실패 (출금은 정상 처리됨) - 기기 토큰이 없거나 테이블 오류");
                }

            } catch (Exception e) {
                log.error("자동이체 실패 - Subscription ID: {}, 사유: {}", sub.getSubscriptionId(), e.getMessage());

                try {
                    sendPaymentNotification(userId, "PAY_FAIL");
                } catch (Exception ex) {
                    log.warn("실패 알림 발송 실패");
                }
            }
        }
        log.info("[자동이체 배치 종료] 총 {}건 처리", targetList.size());
    }

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

    private boolean isBusinessDay(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
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