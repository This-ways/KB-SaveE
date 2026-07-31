package org.scoula.savings.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationRequestDTO { //사용자 입장에서 추천 요청
    private Long monthlyAmount; // 월 납입액 (예: 87000)
    private Integer saveTerm;   // 목표 기간 (예: 12)

    @Builder.Default
    private String productType = "ALL";
}
