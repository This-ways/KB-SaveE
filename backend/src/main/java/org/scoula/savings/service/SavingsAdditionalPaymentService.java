package org.scoula.savings.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.savings.domain.PaymentVO;
import org.scoula.savings.domain.SubscriptionVO;
import org.scoula.savings.dto.DepositReqDTO;
import org.scoula.savings.dto.DepositResDTO;
import org.scoula.savings.mapper.AccountMapper;
import org.scoula.savings.mapper.SavingsMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Log4j2
@Service
@RequiredArgsConstructor
public class SavingsAdditionalPaymentService {

    private final SavingsMapper savingsMapper;
    private final AccountMapper accountMapper;

    @Transactional
    public DepositResDTO processDeposit(Long userId, Long subscriptionId, DepositReqDTO req) {
        // 1. 입력 값 직접 검증
        if (req == null || req.getAmount() == null) {
            throw new IllegalArgumentException("납입 금액을 입력해 주세요.");
        }
        if (req.getAmount() < 1000) {
            throw new IllegalArgumentException("최소 납입 금액은 1,000원 이상입니다.");
        }

        // 2. 적금 계좌 정보 및 본인 소유 확인
        SubscriptionVO sub = savingsMapper.selectSubscriptionWithProduct(subscriptionId);
        if (sub == null) {
            throw new IllegalArgumentException("존재하지 않는 적금 계좌입니다.");
        }

        // 출금 계좌 소유주(userId) 검증
        Long depositUserId = accountMapper.selectUserIdByDepositId(sub.getDepositId());
        if (depositUserId == null || !depositUserId.equals(userId)) {
            throw new IllegalArgumentException("해당 적금 계좌에 대한 접근 권한이 없습니다.");
        }

        int todayInt = Integer.parseInt(
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        );

        if (sub.getEndDate() != null && todayInt > sub.getEndDate()) {
            throw new IllegalStateException(
                    "만기된 적금은 추가 납입할 수 없습니다. 상품을 해지하여 원금과 이자를 수령해 주세요."
            );
        }

        if (sub.getStatus() != 10) {
            throw new IllegalStateException(
                    "해지된 적금 계좌에는 추가 납입할 수 없습니다."
            );
        }


        //출금 전 월 최대 납입 한도 계산
        Long maxMonthlyAmount = sub.getMaxAmount();

        if (maxMonthlyAmount != null && maxMonthlyAmount > 0) {
            // 이번 달(YYYYMM) 이미 납입한 총액 조회
            String currentYearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
            Long paidThisMonthLong = savingsMapper.selectTotalPaidThisMonth(subscriptionId, currentYearMonth);
            long paidThisMonth = (paidThisMonthLong != null) ? paidThisMonthLong : 0L;

            // 이달 남은 한도 계산
            long remainingMonthlyLimit = maxMonthlyAmount - paidThisMonth;

            if (remainingMonthlyLimit <= 0) {
                throw new IllegalStateException(
                        String.format("이번 달 월 납입 한도(%,d원)를 이미 모두 채웠습니다.", maxMonthlyAmount)
                );
            }

            if (req.getAmount() > remainingMonthlyLimit) {
                throw new IllegalArgumentException(
                        String.format("이번 달 추가 납입 가능 잔여 금액은 최대 %,d원입니다.", remainingMonthlyLimit)
                );
            }
        }


        // 3. 출금 계좌 잔액 확인 및 차감 (출금)
        Long currentBalance = accountMapper.selectBalanceByDepositId(sub.getDepositId());
        if (currentBalance == null || currentBalance < req.getAmount()) {
            throw new IllegalStateException("출금 계좌의 잔액이 부족합니다.");
        }


        // 실제 출금
        int updatedRows = accountMapper.withdrawBalance(sub.getDepositId(), req.getAmount());
        if (updatedRows == 0) {
            throw new IllegalStateException("출금 처리 중 오류가 발생했습니다.");
        }

        // 4. 다음 회차 계산 (현재 최신 회차 + 1)
        Integer maxRound = savingsMapper.selectMaxRoundNo(subscriptionId);
        int nextTurn = (maxRound == null) ? 1 : maxRound + 1;


        PaymentVO payment = PaymentVO.builder()
                .subscriptionId(subscriptionId)
                .roundNo(nextTurn)
                .amount(req.getAmount().longValue())
                .paidAt(todayInt)
                .build();

        savingsMapper.insertPayment(payment);
        log.info("[추가 납입 성공] Subscription ID: {}, 회차: {}회차, 금액: {}원", subscriptionId, nextTurn, req.getAmount());

        // 6. 납입 후 총 원금 조회
        Long totalPrincipalLong = savingsMapper.selectTotalPrincipal(subscriptionId);
        int totalPrincipal = (totalPrincipalLong == null) ? 0 : totalPrincipalLong.intValue();

        return DepositResDTO.builder()
                .subscriptionId(subscriptionId)
                .paymentTurn(nextTurn)
                .depositAmount(req.getAmount())
                .totalPrincipal(totalPrincipal)
                .paymentDate(LocalDate.now())
                .message("추가 납입이 성공적으로 완료되었습니다.")
                .build();
    }
}