package org.scoula.savings.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SavingsStatusResDTO {
    // 1. 상단 카드 (기본 정보)
    private String productName;      // 상품명 (ex: KB스타적금III)
    private Long productId;
    private String saveType;         // 적립방식 (ex: 자유적립식)
    private String companyName;      // 은행명 (ex: KB국민은행)
    private double appliedRate;      // 적용 금리
    private int saveTerm;            // 적립 기간 (개월)

    // 2. 중단 카드 (상세 현황)
    private LocalDate startDate;     // 가입일
    private Long monthlyAmount;      // 월 납입액 (설정금액)
    private Long expectedAmount;     // 만기 시 예상 수령액 (원금 + 이자)
    private Long totalPrincipal;     // 총 납입 원금 (실제 납입한 금액 합산)
    private int achievementRate;     // 목표 달성률 (%) -  기간 기반 계산
    private LocalDate nextPaymentDate; // 다음 납입 예정일

    // 3. 하단 차트 (월별 납입 현황)
    private List<MonthlyPaymentDTO> monthlyPayments;
}