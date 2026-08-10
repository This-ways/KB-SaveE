package org.scoula.savings.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepositResDTO {
    private Long subscriptionId;
    private Integer paymentTurn;       // 몇 회차 납입인지
    private Integer depositAmount;     // 금회 납입 금액
    private Integer totalPrincipal;    // 납입 후 총 원금
    private LocalDate paymentDate;     // 납입일
    private String message;
}