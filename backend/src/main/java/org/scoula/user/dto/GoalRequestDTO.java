package org.scoula.user.dto;

import lombok.Data;

@Data
public class GoalRequestDTO {
    private Long categoryId;
    private String yearMonth;   // "YYYY-MM" - 없으면 서버가 currentYearMonth 사용
    private int targetAmount;
}
