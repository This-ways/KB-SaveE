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
        // 💡 Mapper 반환 타입 변경: SavingsProductDTO -> SavingsProductVO
        SavingsProductVO product = savingsMapper.selectProductById(productId);
        if (product == null) {
            throw new IllegalArgumentException("존재하지 않는 적금 상품입니다. ID: " + productId);
        }

        // 💡 Mapper 반환 타입 변경: SavingsRateDTO -> SavingsRateVO
        List<SavingsRateVO> rates = savingsMapper.selectRatesByProductId(productId);

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
        Double highestRate = 0.0; // 💡 최상단 배너용 최고 금리 저장 변수

        for (SavingsRateVO rate : rates) {
            int term = rate.getSaveTerm();
            String termLabel = (term % 12 == 0) ? (term / 12) + "년제" : term + "개월제";
            termLabels.add(termLabel);

            // 💡 BigDecimal -> Double 변환 후 비교
            Double currentMaxRate = (rate.getMaxRate() != null) ? rate.getMaxRate().doubleValue() : 0.0;
            Double currentMinRate = (rate.getMinRate() != null) ? rate.getMinRate().doubleValue() : 0.0;

            // 기간별 금리 중 가장 높은 금리를 찾아서 highestRate에 세팅
            if (currentMaxRate > highestRate) {
                highestRate = currentMaxRate;
            }

            rateOptionList.add(SavingsDetailDTO.RateOptionDTO.builder()
                    .saveTerm(term)
                    .termLabel(termLabel)
                    .minRate(currentMinRate) // 💡 Double 값으로 매핑
                    .maxRate(currentMaxRate) // 💡 Double 값으로 매핑
                    .build());
        }

        String contractTermsText = String.join(", ", termLabels);

        // 3. 최종 DTO 생성 (bankName과 maxRate 세팅)
        return SavingsDetailDTO.builder()
                .productId(product.getProductId())
                .productName(product.getProductName())
                .productType(product.getProductType())
                .bankName("KB국민은행")            // 💡 은행명 세팅
                .maxRate(highestRate)               // 💡 추출한 최고 연이율 세팅 (예: 3.55)
                .feature(product.getFeature())
                .target(product.getTarget())
                .depositAmountText(depositAmountText)
                .contractTermsText(contractTermsText)
                .interestPayType(product.getInterestPayType())
                .detailUrl(product.getDetailUrl())
                .rateList(rateOptionList)
                .build();
    }
}