package org.scoula.savings.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class SavingsSubscribeResDTO {
    private LocalDate endDate;          // 만기일
    private String productName;         // 상품명
    private Integer depositAmount;      // 가입금액
    private BigDecimal appliedRate;     // 적용금리
    private BigDecimal baseRate;        // 기본금리
    private String savingsAccountNo;    // 적금계좌번호 (랜덤생성)
    private Integer paymentDay;         // 자동이체일
    private Integer autoTransferAmount; // 자동이체금액
    private LocalDate startDate;        // 계약체결일(시작일)
}