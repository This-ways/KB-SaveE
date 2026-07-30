package org.scoula.transaction.service;

import org.scoula.transaction.domain.TransactionVO;
import org.scoula.transaction.dto.TransactionSummaryDTO;

import java.util.List;

public interface TransactionService {

    // categoryId는 선택값 (null이면 전체 카테고리)
    List<TransactionVO> getList(Long userId, String yearMonth, Long categoryId);

    TransactionSummaryDTO getSummary(Long userId, String yearMonth);
}
