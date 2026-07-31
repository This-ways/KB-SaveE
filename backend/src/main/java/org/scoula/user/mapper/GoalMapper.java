package org.scoula.user.mapper;

import org.apache.ibatis.annotations.Param;
import org.scoula.user.domain.GoalVO;

import java.util.List;

public interface GoalMapper {
    int insert(GoalVO vo);   // UNIQUE(user_id, category_id, year_month) 위반 시 DuplicateKeyException

    int update(GoalVO vo);   // goal_id 기준 target_amount 수정

    List<GoalVO> findByUserAndYearMonth(@Param("userId") Long userId,
                                         @Param("yearMonth") String yearMonth);

    GoalVO findOne(@Param("userId") Long userId,
                   @Param("categoryId") Long categoryId,
                   @Param("yearMonth") String yearMonth);
}
