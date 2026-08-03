package org.scoula.savings.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class SavingsConfirmResDTO {
    private String productName;         // 상품명
    private Integer saveTerm;           // 가입기간
    private LocalDate endDate;          // 만기일
    private Integer depositAmount;      // 가입금액
    private BigDecimal appliedRate;     // 적용금리
    private String saveType;            // 적립방식
    private String depositAccountNo;    // 사용자 예금 계좌번호
    private Integer paymentDay;         // 자동이체일
    private Integer autoTransferAmount; // 자동이체금액
}