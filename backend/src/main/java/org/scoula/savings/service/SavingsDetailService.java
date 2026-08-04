package org.scoula.savings.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.scoula.savings.domain.SavingsProductVO;
import org.scoula.savings.domain.SavingsRateVO;
import org.scoula.savings.dto.SavingsDetailDTO;
import org.scoula.savings.mapper.SavingsMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SavingsDetailService {

    private final SavingsMapper savingsMapper;

    public SavingsDetailDTO getSavingsDetail(Long productId) {
        // SavingsProductVO
        SavingsProductVO product = savingsMapper.selectProductById(productId);
        if (product == null) {
            throw new IllegalArgumentException("존재하지 않는 적금 상품입니다. ID: " + productId);
        }

        // SavingsRateVO
        List<SavingsRateVO> rates = savingsMapper.selectRatesByProductId(productId);

        // 금리 기준일
        String formattedDate = "";
        if (rates != null && !rates.isEmpty()) {
            Integer dbStartDate = rates.get(0).getStartDate();
            if (dbStartDate != null) {
                formattedDate = String.valueOf(dbStartDate).replaceAll("(\\d{4})(\\d{2})(\\d{2})", "$1.$2.$3");
            }
        }

        // 1. 저축금액 텍스트 가공
        String depositAmountText = "제한없음";
        if (product.getMaxAmount() != null && product.getMaxAmount() > 0) {
            long min = (product.getMinAmount() != null) ? product.getMinAmount() : 0;
            long max = product.getMaxAmount();
            depositAmountText = String.format("최소 %,d원 ~ 최대 %,d원", min, max);
        }

        // 2. 계약기간 요약 텍스트 및 금리표 데이터 구성 + [전체 기간 중 최고 금리 추출]
        List<String> termLabels = new ArrayList<>();
        List<SavingsDetailDTO.RateOptionDTO> rateOptionList = new ArrayList<>();
        Double highestRate = 0.0; // 최상단 배너용 최고 금리 저장 변수

        for (SavingsRateVO rate : rates) {
            int term = rate.getSaveTerm();
            String termLabel = (term % 12 == 0) ? (term / 12) + "년제" : term + "개월제";
            termLabels.add(termLabel);

            // BigDecimal -> Double 변환 후 비교
            Double currentMaxRate = (rate.getMaxRate() != null) ? rate.getMaxRate().doubleValue() : 0.0;
            Double currentMinRate = (rate.getMinRate() != null) ? rate.getMinRate().doubleValue() : 0.0;

            // 기간별 금리 중 가장 높은 금리를 찾아서 highestRate에 세팅
            if (currentMaxRate > highestRate) {
                highestRate = currentMaxRate;
            }

            rateOptionList.add(SavingsDetailDTO.RateOptionDTO.builder()
                    .saveTerm(term)
                    .termLabel(termLabel)
                    .minRate(currentMinRate) // Double 값으로 매핑
                    .maxRate(currentMaxRate) // Double 값으로 매핑
                    .build());
        }

        String contractTermsText = String.join(", ", termLabels);

        // 3. 최종 DTO 생성 (bankName과 maxRate 세팅)
        return SavingsDetailDTO.builder()
                .productId(product.getProductId())
                .productName(product.getProductName())
                .productType(product.getProductType())
                .bankName(getCompanyName(product.getCompanyCode())) // 동적 은행명 매핑 적용
                .maxRate(highestRate)               // 추출한 최고 연이율 세팅 (예: 3.55)
                .rateStartDate(formattedDate)
                .feature(product.getFeature())
                .target(product.getTarget())
                .depositAmountText(depositAmountText)
                .contractTermsText(contractTermsText)
                .interestPayType(product.getInterestPayType())
                .detailUrl(product.getDetailUrl())
                .rateList(rateOptionList)
                .build();
    }

    private String getCompanyName(String companyCode) {
        if (companyCode == null) return "알 수 없음";

        switch (companyCode) {
            case "0010927": return "KB국민은행";
//            case "0010037": return "전북은행";
//            case "0010003": return "IBK기업은행";
//            case "0010020": return "우리은행";
//            case "0010081": return "하나저축은행";
//            case "0010011": return "NH투자증권";
            default: return "기타은행";
        }
    }
}