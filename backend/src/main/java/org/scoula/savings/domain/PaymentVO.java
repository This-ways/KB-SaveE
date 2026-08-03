package org.scoula.savings.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentVO {
    private Long paymentId;        // PK
    private Long subscriptionId;   // 가입한 적금 PK (외래키)

    private Integer roundNo;       // 회차 (round_no)
    private Long amount;           // 납입 금액 (amount)
    private Integer paidAt;        // 납입일 (paid_at)

    private LocalDateTime createdAt; // 생성일시
    private LocalDateTime updatedAt; // 수정일시

}