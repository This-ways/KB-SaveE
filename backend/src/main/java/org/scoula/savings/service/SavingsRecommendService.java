package org.scoula.savings.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.scoula.savings.dto.RecommendationRequestDTO;
import org.scoula.savings.dto.SavingsRecommendDTO;
import org.scoula.savings.mapper.SavingsMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SavingsRecommendService {

    private final SavingsMapper savingsMapper;

    /**
     * 사용자가 입력한 목표기간(saveTerm)과 월납입액(monthlyAmount)을 바탕으로
     * 가입 한도 조건을 만족하는 상품만 예상 세후 수령액을 계산하여 최고 금리순으로 반환
     */
    public List<SavingsRecommendDTO> getRecommendedSavings(RecommendationRequestDTO request) {
        long monthlyAmount = request.getMonthlyAmount();
        int term = request.getSaveTerm();

        // 실무적 방어 코드: 월 납입액 최소 유효성 검사
        if (monthlyAmount < 1000) {
            throw new IllegalArgumentException("적금 월 납입액은 최소 1,000원 이상이어야 합니다. 입력값: " + monthlyAmount + "원");
        }

        // Mapper 호출 시 term(기간)과 monthlyAmount(월 납입액)를 같이 전달하여 한도 내 상품만 조회
        List<SavingsRecommendDTO> productList = savingsMapper.selectRecommendedProducts(term, monthlyAmount);

        List<SavingsRecommendDTO> result = new ArrayList<>();

        for (SavingsRecommendDTO item : productList) {
            // 총 원금 계산
            long totalPrincipal = monthlyAmount * term;

            // 세전 단리 이자 계산: (월납입액 * n(n+1) / 2) * (연이율 / 100) / 12
            double annualRate = (item.getMaxRate() != null) ? item.getMaxRate() : 0.0;
            double preTaxInterest = (monthlyAmount * term * (term + 1) / 2.0) * (annualRate / 100.0) / 12.0;

            // 세후 이자 계산 (이자소득세 15.4% 공제)
            long postTaxInterest = Math.round(preTaxInterest * (1 - 0.154));

            // 최종 수령액
            long finalAmount = totalPrincipal + postTaxInterest;

            // 계산된 원금, 이자, 최종 수령액 세팅
            item.setTotalPrincipal(totalPrincipal);
            item.setExpectedInterest(postTaxInterest);
            item.setFinalReceiveAmount(finalAmount);

            result.add(item);
        }

        return result;
    }
}