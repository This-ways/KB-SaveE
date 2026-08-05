package org.scoula.savings.service;

import lombok.RequiredArgsConstructor;
import org.scoula.savings.domain.PaymentVO;
import org.scoula.savings.domain.SubscriptionVO;
import org.scoula.savings.dto.CancelPreviewResDTO;
import org.scoula.savings.mapper.AccountMapper;
import org.scoula.savings.mapper.SavingsMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SavingsCancelService {

    private final SavingsMapper savingsMapper;
    private final AccountMapper accountMapper;

    // 1. 해지 예상 명세서 데이터 조회
    public CancelPreviewResDTO getCancelPreview(Long userId, Long subscriptionId) {
        SubscriptionVO sub = savingsMapper.selectSubscriptionWithProduct(subscriptionId);

        // 계좌 존재 여부 검증
        if (sub == null) {
            throw new IllegalArgumentException("존재하지 않는 적금 계좌입니다.");
        }

        // [보안 추가] 해당 적금 계좌가 현재 로그인한 유저의 계좌인지 검증
        // (SubscriptionVO에 연동된 depositId 등을 통해 user를 판별하거나, Mapper에서 유저 소유 여부를 확인해야 합니다)
        // 예: 계좌 소유자 검증 로직 (AccountMapper나 SavingsMapper에 소유자 확인 쿼리가 있다면 연동)
        validateSubscriptionOwner(userId, sub);

        // 이미 해지된 계좌인지 상태값(status) 검증 (10: 정상, 90: 해지)
        if (sub.getStatus() == 90) {
            throw new IllegalArgumentException("이미 해지 처리된 적금 계좌입니다.");
        }

        Long totalPrincipal = savingsMapper.selectTotalPrincipal(subscriptionId);
        if (totalPrincipal == null) totalPrincipal = 0L;

        LocalDate startDate = parseDate(sub.getStartDate());
        LocalDate endDate = parseDate(sub.getEndDate());
        LocalDate today = LocalDate.now();

        int contractMonths = sub.getUserSaveTerm();
        long elapsedMonths = ChronoUnit.MONTHS.between(startDate, today);
        double appliedRate = sub.getAppliedRate().doubleValue();

        String cancelType;
        String rateLabel;
        double finalRate = 0.0;

        // [핵심] 해지 종류 판별 및 이율 적용
        if (today.isBefore(endDate)) {
            cancelType = "중도해지";
            rateLabel = "중도해지 이율";
            finalRate = calculateMidTermCancelRate(appliedRate, contractMonths, (int) elapsedMonths);
        } else if (today.isEqual(endDate) || today.isBefore(endDate.plusMonths(1))) {
            cancelType = "만기해지";
            rateLabel = "약정 이율 (연 " + appliedRate + "%)";
            finalRate = appliedRate;
        } else {
            cancelType = "만기후해지";
            rateLabel = "만기 후 이율";
            long passedMonthsAfterMaturity = ChronoUnit.MONTHS.between(endDate, today);
            finalRate = calculatePostMaturityRate(appliedRate, (int) passedMonthsAfterMaturity);
        }

        // 소수점 셋째 자리 절사
        finalRate = Math.floor(finalRate * 100) / 100.0;

        List<PaymentVO> payments = savingsMapper.selectAllPayments(subscriptionId);
        long totalPreTaxInterest = 0L;

        for (PaymentVO payment : payments) {
            LocalDate paymentDate = parseDate(payment.getPaidAt());
            long elapsedDays = ChronoUnit.DAYS.between(paymentDate, today);

            if (elapsedDays > 0) {
                double interestForThisPayment = payment.getAmount() * (finalRate / 100.0) * (elapsedDays / 365.0);
                totalPreTaxInterest += (long) interestForThisPayment;
            }
        }

        long taxAmount = (long) (totalPreTaxInterest * 0.154);
        long actualReceiveAmount = totalPrincipal + totalPreTaxInterest - taxAmount;

        return CancelPreviewResDTO.builder()
                .cancelType(cancelType)
                .totalPrincipal(totalPrincipal)
                .rateLabel(rateLabel)
                .appliedCancelRate(finalRate)
                .preTaxInterest(totalPreTaxInterest)
                .taxAmount(taxAmount)
                .actualReceiveAmount(actualReceiveAmount)
                .build();
    }

    // 2. 실제 해지 처리 (트랜잭션 필수)
    @Transactional
    public void cancelSubscription(Long userId, Long subscriptionId) {
        // 1. 최종 수령액 계산 (내부에서 소유자 검증도 함께 수행됨)
        CancelPreviewResDTO previewDTO = getCancelPreview(userId, subscriptionId);
        SubscriptionVO sub = savingsMapper.selectSubscriptionWithProduct(subscriptionId);

        // 2. 적금 상태를 90(해지)로 업데이트
        savingsMapper.updateSubscriptionCancelStatus(subscriptionId, 90);

        // 3. 연결된 입출금 통장에 실 수령액을 입금
        savingsMapper.updateAccountBalance(sub.getDepositId(), previewDTO.getActualReceiveAmount());
    }

    // [보안 검증 메서드 예시] 적금 계좌의 주인이 로그인한 유저가 맞는지 확인
    private void validateSubscriptionOwner(Long userId, SubscriptionVO sub) {
        // sub.getDepositId()를 통해 예금 계좌를 조회한 뒤, 그 예금 계좌의 소유자(userId)가 일치하는지 확인하는 로직 수행
        Long ownerId = accountMapper.selectUserIdByDepositId(sub.getDepositId());

        if (ownerId == null || !ownerId.equals(userId)) {
            throw new IllegalArgumentException("본인의 적금 계좌만 조회/해지할 수 있습니다.");
        }
    }

    // 중도해지 구간별 이율 계산기
    private double calculateMidTermCancelRate(double appliedRate, int contractMonths, int elapsedMonths) {
        if (elapsedMonths < 1) return 0.1;

        double ratio = (double) elapsedMonths / contractMonths;
        double rate;

        if (elapsedMonths < 6) {
            rate = appliedRate * 0.5 * ratio;
            return Math.max(rate, 0.1);
        } else if (elapsedMonths < 8) {
            rate = appliedRate * 0.6 * ratio;
        } else if (elapsedMonths < 10) {
            rate = appliedRate * 0.7 * ratio;
        } else if (elapsedMonths < 11) {
            rate = appliedRate * 0.8 * ratio;
        } else {
            rate = appliedRate * 0.9 * ratio;
        }
        return Math.max(rate, 0.2);
    }

    // 만기 후 구간별 이율 계산기
    private double calculatePostMaturityRate(double appliedRate, int passedMonthsAfterMaturity) {
        if (passedMonthsAfterMaturity <= 1) return appliedRate * 0.5;
        if (passedMonthsAfterMaturity <= 3) return appliedRate * 0.3;
        return 0.1;
    }

    private LocalDate parseDate(int dateInt) {
        return LocalDate.parse(String.valueOf(dateInt), DateTimeFormatter.ofPattern("yyyyMMdd"));
    }
}