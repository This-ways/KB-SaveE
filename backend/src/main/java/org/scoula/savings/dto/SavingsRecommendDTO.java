package org.scoula.savings.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingsRecommendDTO {
    private Long productId;
    private String productName;
    private String productType;
    private String companyCode;

    private Double maxRate;          // 최고 연이율 (%)
    private Long totalPrincipal;     // 총 원금 (예: 1,044,000원)
    private Long expectedInterest;   // 예상 세후 이자
    private Long finalReceiveAmount; // 최종 예상 수령액
}

