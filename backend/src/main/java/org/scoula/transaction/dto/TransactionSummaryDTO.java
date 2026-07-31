package org.scoula.transaction.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

// GET /api/transactions/summary 응답 전체 구조
// 상위 5개 카테고리 + 나머지(remainder)로 구성
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransactionSummaryDTO {
    private String yearMonth;
    private Integer totalAmount;
    private List<CategoryAmountDTO> categories; // 상위 5개 (또는 5개 미만이면 전체)
    private Remainder remainder;                // 6위 이하 합계, 없으면 null

    // 클래스 안에 또 클래스를 정의한 것 (static 중첩 클래스)
    // "나머지"는 TransactionSummaryDTO 안에서만 쓰이는 전용 구조라, 별도 파일로 안 빼고 여기에 묶어둠
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Remainder {
        private String label; // "나머지" 고정값
        private Integer amount;
    }
}
