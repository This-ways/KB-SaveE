package org.scoula.report.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 이번 달 수입/지출 합계 (현금흐름 계산의 재료, Service에서 전월잔액과 조합됨)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IncomeExpenseDTO {
    private Integer income;
    private Integer expense;
}
