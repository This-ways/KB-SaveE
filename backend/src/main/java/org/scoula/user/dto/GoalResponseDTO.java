package org.scoula.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.scoula.user.domain.GoalVO;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoalResponseDTO {
    private Long goalId;
    private Long categoryId;
    private String yearMonth;
    private int targetAmount;

    public static GoalResponseDTO of(GoalVO vo) {
        return new GoalResponseDTO(vo.getGoalId(), vo.getCategoryId(), vo.getYearMonth(), vo.getTargetAmount());
    }
}
