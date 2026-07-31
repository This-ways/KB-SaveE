package org.scoula.transaction.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.transaction.domain.TransactionVO;
import org.scoula.transaction.dto.CategoryAmountDTO;
import org.scoula.transaction.dto.TransactionSummaryDTO;
import org.scoula.transaction.mapper.TransactionMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Log4j2
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private static final int TOP_N = 5;

    private final TransactionMapper mapper;

    @Override
    public List<TransactionVO> getList(Long userId, String yearMonth, Long categoryId) {
        return mapper.getList(userId, yearMonth, categoryId);
    }

    @Override
    public TransactionSummaryDTO getSummary(Long userId, String yearMonth) {
        // 1. 전체 카테고리별 합계를 금액 큰 순으로 다 받아옴 (Mapper가 이미 정렬해서 줌)
        List<CategoryAmountDTO> all = mapper.getCategorySummary(userId, yearMonth);

        // 2. 전체 지출 총액 = 모든 카테고리 합계를 다 더한 값
        int totalAmount = all.stream().mapToInt(CategoryAmountDTO::getAmount).sum();

        // 3. 상위 5개만 잘라냄 (5개보다 적으면 있는 만큼만)
        List<CategoryAmountDTO> top5 = all.stream().limit(TOP_N).toList();

        // 4. 6번째부터 끝까지의 금액을 합쳐서 "나머지" 한 줄로 만듦
        TransactionSummaryDTO.Remainder remainder = null;
        if (all.size() > TOP_N) {
            int remainderAmount = all.stream()
                    .skip(TOP_N)
                    .mapToInt(CategoryAmountDTO::getAmount)
                    .sum();
            remainder = new TransactionSummaryDTO.Remainder("나머지", remainderAmount);
        }

        return new TransactionSummaryDTO(yearMonth, totalAmount, top5, remainder);
    }

    @Override
    public void updateCategory(Long txnId, Long userId, Long categoryId) {
        int updated = mapper.updateCategory(txnId, userId, categoryId);
        if (updated == 0) {
            // 존재하지 않거나, 본인 거래가 아니거나, 수입 거래인 경우
            throw new IllegalArgumentException(
                    "카테고리를 수정할 수 없는 거래입니다. (존재하지 않거나, 본인 거래가 아니거나, 수입 거래입니다)"
            );
        }
    }
}
