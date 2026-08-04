package org.scoula.savings.mapper;


import org.apache.ibatis.annotations.Mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.scoula.savings.domain.PaymentVO;
import org.scoula.savings.domain.SavingsProductVO;
import org.scoula.savings.domain.SavingsRateVO;
import org.scoula.savings.domain.SubscriptionVO;
import org.scoula.savings.dto.MonthlyPaymentDTO;
import org.scoula.savings.dto.SavingsRecommendDTO;


@Mapper
public interface SavingsMapper {
    int upsertProduct(SavingsProductVO product);
    int deleteRatesByProductId(Long productId);
    int insertRate(SavingsRateVO rate);
    List<SavingsRecommendDTO> selectRecommendedProducts(
            @Param("saveTerm") int saveTerm,
            @Param("monthlyAmount") long monthlyAmount
    );
    SavingsProductVO selectProductById(Long productId);
    List<SavingsRateVO> selectRatesByProductId(Long productId);

    void insertSubscription(SubscriptionVO subscription); //적금 가입 시 insert

    SavingsRateVO selectRateByTerm(
            @Param("productId") Long productId,
            @Param("saveTerm") int saveTerm
    );

    void insertPayment(PaymentVO payment); //납입내역 저장

    // 총 납입 원금 합산 (단일 값 반환이므로 Long)
    Long selectTotalPrincipal(@Param("subscriptionId") Long subscriptionId);

    // 하단 차트용 월별 납입 데이터 추출 (여러 행이 반환되므로 List 사용)
    List<MonthlyPaymentDTO> selectMonthlyPayments(@Param("subscriptionId") Long subscriptionId);

    // 가입 내역과 상품 정보(상품명 등)를 조인하여 조회
    SubscriptionVO selectSubscriptionWithProduct(@Param("subscriptionId") Long subscriptionId);

    // 마지막 납입일(가장 최근 결제일) 조회
    Integer selectLastPaymentDate(@Param("subscriptionId") Long subscriptionId);

    Integer selectPaidRounds(@Param("subscriptionId") Long subscriptionId); //납입회차

    //  적금 상태 업데이트 (해지 처리)
    int updateSubscriptionCancelStatus(@Param("subscriptionId") Long subscriptionId, @Param("status") int status);

    // 사용자의 입출금 계좌 잔액 업데이트 (실 수령액 입금)
    int updateAccountBalance(@Param("depositId") Long depositId, @Param("amount") Long amount);

    // 💡 [추가] 회차별 납입 상세 내역 조회 (건별 이자 계산용)
    List<org.scoula.savings.domain.PaymentVO> selectAllPayments(@Param("subscriptionId") Long subscriptionId);
}
