package org.scoula.user.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoalVO {
    private Long goalId;
    private Long userId;
    private Long categoryId;
    private String yearMonth;   // "YYYY-MM"
    private int targetAmount;
}
