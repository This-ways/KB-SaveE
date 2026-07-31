package org.scoula.savings.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavingsRateVO {
    private Long rateId;
    private Long productId;
    private Integer saveTerm;
    private BigDecimal minRate;
    private BigDecimal maxRate;
    private Integer startDate;
    private Integer endDate;
}