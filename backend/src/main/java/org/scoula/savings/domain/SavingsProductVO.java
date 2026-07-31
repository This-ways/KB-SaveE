package org.scoula.savings.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavingsProductVO {
    private Long productId;
    private String companyCode;
    private String productCode;
    private String productName;
    private String productType;
    private Integer minAmount;
    private Long maxAmount;
    private String feature;
    private String target;
    private String interestPayType;
    private String detailUrl;
    private Integer status;
}