package org.scoula.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryAverageDTO {
    private Long categoryId;
    private Integer avgAmount;  // 최근 N개월 카테고리별 평균 지출(월 기준)
}
