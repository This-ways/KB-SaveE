package org.scoula.transaction.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 도넛 차트 등에서 쓰는 "카테고리별 지출 합계" 한 줄
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryAmountDTO {
    private Long categoryId;
    private String categoryName;
    private Integer amount;
}
