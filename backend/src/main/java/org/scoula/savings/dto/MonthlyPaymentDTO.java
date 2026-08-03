// 차트 데이터를 담을 내부 DTO (MonthlyPaymentDTO.java)
package org.scoula.savings.dto;

import lombok.Data;

@Data
public class MonthlyPaymentDTO {
    private int payYear;
    private int payMonth;  // 납입 월 (ex: 1, 2, 3...)
    private Long amount;   // 해당 월의 총 납입액
}