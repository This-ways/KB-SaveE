package org.scoula.savings.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SavingsSubscribeReqDTO {
    private Long userId;             // 사용자 ID
    private Long productId;          // 적금상품 ID
    private String saveType;         // 적립방식 (자유적립식 / 정액적립식)
    private Integer saveTerm;        // 가입기간 (개월)
    private Integer depositAmount;   // 가입금액 (초기/월 납입액)
    private Integer paymentDay;      // 자동이체일 (매월 OO일)
    private Integer autoTransferAmount; // 자동이체금액
}