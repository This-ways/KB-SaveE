package org.scoula.savings.service;

import lombok.RequiredArgsConstructor;
import org.scoula.savings.domain.PaymentVO;
import org.scoula.savings.domain.SubscriptionVO;
import org.scoula.savings.mapper.AccountMapper;
import org.scoula.savings.mapper.SavingsMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class AutoTransferProcessor {

    private final SavingsMapper savingsMapper;
    private final AccountMapper accountMapper;

    @Transactional
    public void processSingleTransfer(SubscriptionVO sub) {

        Long amount = Long.valueOf(sub.getMonthlyAmount());

        int updated = accountMapper.withdrawBalance(sub.getDepositId(), amount);

        if (updated == 0) {
            throw new RuntimeException("출금 계좌 잔액 부족");
        }

        Integer maxRound = savingsMapper.selectMaxRoundNo(sub.getSubscriptionId());
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
}