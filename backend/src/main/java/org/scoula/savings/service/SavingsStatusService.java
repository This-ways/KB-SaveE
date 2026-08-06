package org.scoula.savings.service;

import lombok.RequiredArgsConstructor;
import org.scoula.savings.domain.SubscriptionVO;
import org.scoula.savings.dto.MonthlyPaymentDTO;
import org.scoula.savings.dto.SavingsStatusResDTO;
import org.scoula.savings.mapper.AccountMapper;
import org.scoula.savings.mapper.SavingsMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SavingsStatusService {

    private final SavingsMapper savingsMapper;
    private final AccountMapper accountMapper;

    public SavingsStatusResDTO getSavingsStatus(Long userId, Long subscriptionId) {
        // 가입 정보 및 상품 정보 조인 조회
        SubscriptionVO sub = savingsMapper.selectSubscriptionWithProduct(subscriptionId);

        // 계좌 존재 여부 및 해지 상태 검증
        if (sub == null) {
            throw new IllegalArgumentException("존재하지 않는 적금 계좌입니다.");
        }

        // 본인 소유 계좌 검증
        Long ownerId = accountMapper.selectUserIdByDepositId(sub.getDepositId());
        if (ownerId == null || !ownerId.equals(userId)) {
            throw new IllegalArgumentException("본인의 적금 계좌 현황만 조회할 수 있습니다.");
        }

        // 해지 계좌 검증
        if (sub.getStatus() == 90) {
            throw new IllegalArgumentException("이미 해지 처리된 적금 계좌입니다.");
        }

        // 날짜 변환 (DB의 int 20260724 -> LocalDate)
        LocalDate startDate = parseDate(sub.getStartDate());
        LocalDate endDate = parseDate(sub.getEndDate());
        LocalDate today = LocalDate.now();

        // ==========================================
        // 총 납입 원금 및 월별 납입 내역 (차트용) 조회
        // ==========================================
        Long totalPrincipal = savingsMapper.selectTotalPrincipal(subscriptionId);
        if (totalPrincipal == null) {
            totalPrincipal = 0L;
        }

        // ==========================================
        // 실제 납입 회차 기반 목표 달성률 계산
        // ==========================================
        // DB에서 직접 납입 횟수(COUNT)를 가져옵니다.
        Integer paidRounds = savingsMapper.selectPaidRounds(subscriptionId);
        if (paidRounds == null) paidRounds = 0;

        int achievementRate = 0;
        if (sub.getUserSaveTerm() > 0) {
            // 전체 목표 횟수 = 가입 개월 수
            int totalRounds = sub.getUserSaveTerm();

            // 달성률 계산: (현재 납입 횟수 / 전체 납입 횟수) * 100
            achievementRate = (int) (((double) paidRounds / totalRounds) * 100);

            // 달성률 예외 처리 (0% 미만이나 100% 초과 방지)
            achievementRate = Math.max(0, Math.min(100, achievementRate));
        }

        List<MonthlyPaymentDTO> chartData = savingsMapper.selectMonthlyPayments(subscriptionId);


        // 마지막 납입일 기반 다음 납입일 계산
        Integer lastPaymentDateInt = savingsMapper.selectLastPaymentDate(subscriptionId);
        LocalDate nextPaymentDate;

        if (lastPaymentDateInt != null) {
            // 1. 마지막 납입일이 존재하면 그 날짜에서 1개월 추가
            LocalDate lastDate = parseDate(lastPaymentDateInt);
            LocalDate targetMonth = lastDate.plusMonths(1);

            // 2월 30일 같은 오류를 막기 위해 해당 월의 마지막 날짜 확인
            int maxDay = targetMonth.lengthOfMonth();
            int safePaymentDay = Math.min(sub.getPaymentDay(), maxDay);

            nextPaymentDate = targetMonth.withDayOfMonth(safePaymentDay);
        } else {
            // 한 번도 납입하지 않은 경우 가입일(startDate) 기준 1개월 뒤로 설정
            LocalDate targetMonth = startDate.plusMonths(1);
            int maxDay = targetMonth.lengthOfMonth();
            int safePaymentDay = Math.min(sub.getPaymentDay(), maxDay);

            nextPaymentDate = targetMonth.withDayOfMonth(safePaymentDay);
        }

        // 예상 수령액 계산 (단리 계산)
        long expectedAmount = calculateExpectedAmount(
                Long.valueOf(sub.getMonthlyAmount()),
                sub.getAppliedRate().doubleValue(),
                sub.getUserSaveTerm()
        );

        return SavingsStatusResDTO.builder()
                .productName(sub.getProductName())
                .saveType(sub.getUserSaveType())
                .companyName(getCompanyName(sub.getCompanyCode()))
                .appliedRate(sub.getAppliedRate().doubleValue())
                .saveTerm(sub.getUserSaveTerm())
                .startDate(startDate)
                .monthlyAmount(Long.valueOf(sub.getMonthlyAmount()))
                .expectedAmount(expectedAmount)
                .totalPrincipal(totalPrincipal)
                .achievementRate(achievementRate)
                .nextPaymentDate(nextPaymentDate)
                .monthlyPayments(chartData)
                .build();
    }

    private LocalDate parseDate(int dateInt) {
        return LocalDate.parse(String.valueOf(dateInt), DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    // 이자 계산기 (단리 기준, 세후)
    private long calculateExpectedAmount(Long monthlyAmount, double rate, int term) {
        if (monthlyAmount == null || monthlyAmount == 0) return 0;

        // 1. 만기 시 총 납입 원금 = 월 납입액 * 가입 개월 수
        long totalExpectedPrincipal = monthlyAmount * term;

        // 2. 세전 총 이자 (비과세 기준)
        double preTaxInterest = monthlyAmount * (rate / 100) * (term * (term + 1)) / 24.0;

        // 3. 이자소득세 15.4% 차감 (세후 이자)
        double afterTaxInterest = preTaxInterest * (1 - 0.154);

        // 4. 최종 예상 수령액 = 총 원금 + 세후 이자 (소수점 이하는 버림)
        return totalExpectedPrincipal + (long) afterTaxInterest;
    }

    // ==========================================
    // 실제 은행 코드를 한글 은행명으로 변환하는 메서드
    // ==========================================
    private String getCompanyName(String companyCode) {
        if (companyCode == null) return "알 수 없음";

        // DB에 저장된 실제 표준 은행 코드에 맞춰서 반환
        switch (companyCode) {
            case "0010927":
                return "KB국민은행";
            // 나중에 다른 은행 상품이 추가되면 아래에 case를 계속 늘리기
            // case "0010037": return "전북은행";
            // case "0010003": return "IBK기업은행";
            default:
                return "기타은행";
        }
    }
}