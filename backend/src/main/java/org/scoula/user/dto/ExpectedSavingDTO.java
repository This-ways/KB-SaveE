package org.scoula.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpectedSavingDTO {
    private int expectedSaving; // Σ(카테고리별 평균 지출 - 목표금액), 음수 카테고리는 0 처리
}
