package org.scoula.savings.mapper;


import org.apache.ibatis.annotations.Mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.scoula.savings.domain.PaymentVO;
import org.scoula.savings.domain.SavingsProductVO;
import org.scoula.savings.domain.SavingsRateVO;
import org.scoula.savings.domain.SubscriptionVO;
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
}
