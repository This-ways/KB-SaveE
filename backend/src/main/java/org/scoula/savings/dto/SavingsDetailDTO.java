package org.scoula.savings.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingsDetailDTO {
    // 1. 최상단 배너 및 헤더 정보
    private Long productId;            // 상품 ID
    private String productName;        // 상품명 (예: KB내맘대로적금)
    private String productType;        // 적립방식 (예: 자유적립식)
    private String bankName;           // 은행명 (예: KB국민은행)
    private Double maxRate;            // 최고 연이율 (예: 2.6)

    private String rateStartDate; //금리 기준일?

    // 2. 중간 기본 정보 표 (DB 매핑)
    private String feature;            // 상품특징
    private String target;             // 가입대상
    private String depositAmountText;  // 저축금액 (예: 제한없음 또는 월 1만원 ~ 1백만원)
    private String contractTermsText;  // 계약기간 (예: 1년제, 2년제, 3년제)
    private String interestPayType;    // 이자지급시기 (예: 만기일시지급식)
    private String detailUrl;          // 상세 URL

    // 3. 금리 안내 표 리스트
    private List<RateOptionDTO> rateList;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RateOptionDTO {
        private Integer saveTerm;      // 개월 수 (예: 12)
        private String termLabel;      // 표시용 기간 (예: 1년제)
        private Double minRate;        // 최저 연이율 (%)
        private Double maxRate;        // 최고 연이율 (%)
    }
}
