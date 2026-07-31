package org.scoula.savings.mapper;


import org.apache.ibatis.annotations.Mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.scoula.savings.domain.SavingsProductVO;
import org.scoula.savings.domain.SavingsRateVO;
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
}
