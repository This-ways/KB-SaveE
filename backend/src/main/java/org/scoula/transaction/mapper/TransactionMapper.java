package org.scoula.transaction.mapper;

import org.apache.ibatis.annotations.Param;
import org.scoula.transaction.domain.TransactionVO;
import org.scoula.transaction.dto.CategoryAmountDTO;

import java.util.List;

public interface TransactionMapper {

    // 월별 거래내역 목록 (categoryId가 null이면 전체 카테고리 조회, 지출만)
    List<TransactionVO> getList(
            @Param("userId") Long userId,
            @Param("yearMonth") String yearMonth,
            @Param("categoryId") Long categoryId
    );

    // 월별 카테고리별 지출 합계 (지출만, 월세 제외, 금액 큰 순 - 전체 카테고리 다 줌, 5개 자르기는 Service에서)
    List<CategoryAmountDTO> getCategorySummary(
            @Param("userId") Long userId,
            @Param("yearMonth") String yearMonth
    );
}
