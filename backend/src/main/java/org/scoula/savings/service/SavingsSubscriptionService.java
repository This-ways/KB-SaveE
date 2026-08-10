package org.scoula.savings.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.savings.domain.PaymentVO;
import org.scoula.savings.domain.SavingsProductVO;
import org.scoula.savings.domain.SavingsRateVO;
import org.scoula.savings.domain.SubscriptionVO;
import org.scoula.savings.dto.SavingsConfirmResDTO;
import org.scoula.savings.dto.SavingsSubscribeReqDTO;
import org.scoula.savings.dto.SavingsSubscribeResDTO;
import org.scoula.savings.mapper.AccountMapper;
import org.scoula.savings.mapper.SavingsMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Random;

@Log4j2
@Service
@RequiredArgsConstructor
public class SavingsSubscriptionService {

    private final SavingsMapper savingsMapper;
    private final AccountMapper accountMapper;

    /**
     * [STEP 1] 가입 정보 확인 API (Output 1)
     */
    public SavingsConfirmResDTO confirmSubscription(Long userId, SavingsSubscribeReqDTO req) {

        SavingsProductVO product = savingsMapper.selectProductById(req.getProductId());
        if (product == null) throw new IllegalArgumentException("존재하지 않는 상품입니다.");

        SavingsRateVO rate = savingsMapper.selectRateByTerm(req.getProductId(), req.getSaveTerm());
        if (rate == null) throw new IllegalArgumentException("해당 가입기간의 금리 정보가 없습니다.");

        String finalSaveType = determineSaveType(product.getProductType(), req.getSaveType());

        // 계좌 ID 및 계좌번호 조회 (req.getUserId() 대신 파라미터로 받은 userId 사용)
        Long depositId = accountMapper.selectDepositIdByUserId(userId);
        if (depositId == null) throw new IllegalArgumentException("출금할 예금 계좌가 존재하지 않습니다.");

        // 1인 1계좌 제한 검증
        int activeCount = savingsMapper.countActiveSubscriptionByDepositId(depositId);
        if (activeCount > 0) {
            throw new IllegalArgumentException("이미 가입하여 유지 중인 적금 계좌가 존재합니다. (1인 1계좌만 가입 가능)");
        }

        String depositAccountNo = accountMapper.selectAccountNoByUserId(userId);
        LocalDate endDate = LocalDate.now().plusMonths(req.getSaveTerm());
        BigDecimal finalRate = resolveAppliedRate(req.getAppliedRate(), rate);

        return SavingsConfirmResDTO.builder()
                .productName(product.getProductName())
                .saveTerm(req.getSaveTerm())
                .endDate(endDate)
                .depositAmount(req.getDepositAmount())
                .appliedRate(finalRate)
                .saveType(finalSaveType)
                .depositAccountNo(depositAccountNo)
                .paymentDay(req.getPaymentDay())
                .autoTransferAmount(req.getAutoTransferAmount())
                .build();
    }

    /**
     * [STEP 2] 최종 적금 가입 처리 API (Output 2)
     */
    @Transactional
    public SavingsSubscribeResDTO processSubscription(Long userId, SavingsSubscribeReqDTO req) {

        SavingsProductVO product = savingsMapper.selectProductById(req.getProductId());
        if (product == null) throw new IllegalArgumentException("존재하지 않는 상품입니다.");

        SavingsRateVO rate = savingsMapper.selectRateByTerm(req.getProductId(), req.getSaveTerm());
        if (rate == null) throw new IllegalArgumentException("해당 가입기간의 금리 정보가 없습니다.");

        // req.getUserId() 대신 파라미터로 받은 userId 사용
        Long depositId = accountMapper.selectDepositIdByUserId(userId);
        if (depositId == null) throw new IllegalArgumentException("해당 사용자의 예금 계좌를 찾을 수 없습니다.");

        // 1인 1계좌 검증
        int activeCount = savingsMapper.countActiveSubscriptionByDepositId(depositId);
        if (activeCount > 0) {
            throw new IllegalArgumentException("이미 가입하여 유지 중인 적금 계좌가 존재합니다. (1인 1계좌만 가입 가능)");
        }

        // ==========================================
        // 잔액 확인 및 예금 계좌 출금 처리
        // ==========================================
        int updatedCount = accountMapper.withdrawBalance(depositId, req.getDepositAmount());

        if (updatedCount == 0) {
            throw new IllegalArgumentException("출금할 예금 계좌의 잔액이 부족합니다.");
        }

        String finalSaveType = determineSaveType(product.getProductType(), req.getSaveType());

        LocalDate startLocalDate = LocalDate.now();
        LocalDate endLocalDate = startLocalDate.plusMonths(req.getSaveTerm());
        int startDateInt = convertLocalDateToInt(startLocalDate);
        int endDateInt = convertLocalDateToInt(endLocalDate);

        String savingsAccountNo = generateRandomAccount();
        BigDecimal finalRate = resolveAppliedRate(req.getAppliedRate(), rate);

        SubscriptionVO subscription = SubscriptionVO.builder()
                .depositId(depositId)
                .productId(req.getProductId())
                .status(10)
                .startDate(startDateInt)
                .endDate(endDateInt)
                .accountNo(savingsAccountNo)
                .appliedRate(finalRate)
                .monthlyAmount(req.getDepositAmount())
                .userSaveTerm(req.getSaveTerm())
                .userSaveType(finalSaveType)
                .paymentDay(req.getPaymentDay())
                .build();

        // 적금 가입 정보 INSERT
        savingsMapper.insertSubscription(subscription);

        // ==========================================
        // 1회차 납입 내역 (Payment) 기록
        // ==========================================
        PaymentVO payment = PaymentVO.builder()
                .subscriptionId(subscription.getSubscriptionId())
                .roundNo(1)
                .amount(Long.valueOf(req.getDepositAmount()))
                .paidAt(startDateInt)
                .build();

        savingsMapper.insertPayment(payment);

        return SavingsSubscribeResDTO.builder()
                .endDate(endLocalDate)
                .productName(product.getProductName())
                .depositAmount(req.getDepositAmount())
                .appliedRate(finalRate)
                .baseRate(rate.getMinRate())
                .savingsAccountNo(savingsAccountNo)
                .paymentDay(req.getPaymentDay())
                .autoTransferAmount(req.getAutoTransferAmount())
                .startDate(startLocalDate)
                .build();
    }

    private BigDecimal resolveAppliedRate(BigDecimal requestedRate, SavingsRateVO rateVO) {
        if (requestedRate != null && requestedRate.compareTo(BigDecimal.ZERO) > 0) {
            return requestedRate;
        }
        // 요청된 금리가 없으면 해당 기간의 최고금리를 기본값으로 바인딩
        return rateVO.getMaxRate();
    }

    private String determineSaveType(String dbProductType, String requestedType) {
        if (dbProductType != null && dbProductType.contains(",")) {
            if (requestedType == null || requestedType.isEmpty()) {
                throw new IllegalArgumentException("적립 방식을 선택해 주세요.");
            }
            if (!dbProductType.contains(requestedType)) {
                throw new IllegalArgumentException("해당 상품에서 지원하지 않는 적립 방식입니다.");
            }
            return requestedType;
        }
        return dbProductType != null ? dbProductType.trim() : "자유적립식";
    }

    private int convertLocalDateToInt(LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        return Integer.parseInt(date.format(formatter));
    }

    private String generateRandomAccount() {
        Random random = new Random();
        return String.format("810-%04d-%04d-%02d",
                random.nextInt(10000), random.nextInt(10000), random.nextInt(100));
    }
}