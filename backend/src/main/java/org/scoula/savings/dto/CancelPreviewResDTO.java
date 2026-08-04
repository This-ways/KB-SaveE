package org.scoula.savings.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CancelPreviewResDTO {
    private String cancelType;        // 해지 종류 ("중도해지", "만기해지", "만기후해지")
    private Long totalPrincipal;      // 납입 원금

    private String rateLabel;         // 이율 라벨 (ex: "중도해지 이율 (연 0.5%)", "약정 이율 (연 2.6%)")
    private double appliedCancelRate; // 최종 적용된 해지 이율
    private Long preTaxInterest;      // 세전 이자

    private Long taxAmount;           // 이자소득세 (15.4%)
    private Long actualReceiveAmount; // 실 수령액 (원금 + 이자 - 세금)
}
