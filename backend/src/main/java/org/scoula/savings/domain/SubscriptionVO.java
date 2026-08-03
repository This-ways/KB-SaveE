package org.scoula.savings.domain;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionVO {
    private Long subscriptionId;
    private Long depositId;
    private Long productId;

    private Integer status;
    private Integer startDate;
    private Integer endDate;

    private String accountNo;
    private BigDecimal appliedRate;
    private Integer monthlyAmount;
    private Integer userSaveTerm;
    private String userSaveType;
    private Integer paymentDay;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}