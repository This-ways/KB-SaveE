package org.scoula.savings.dto;

import lombok.Data;

@Data
public class UpdateMonthlyAmountReqDTO {
    private Long subscriptionId; // 가입된 적금 ID
    private Long newAmount;      // 새로 설정할 자동이체 금액
}