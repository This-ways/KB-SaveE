package org.scoula.user.service;

import org.scoula.user.dto.GoalRequestDTO;
import org.scoula.user.dto.GoalResponseDTO;

import java.util.List;

public interface GoalService {
    /** 1~7일 수정제한 적용. 이미 있는 (user,category,month) 조합이면 update, 없으면 insert */
    GoalResponseDTO save(Long userId, GoalRequestDTO dto);

    /** "선택한 카테고리들 한번에 확인 후 제출" 화면용 - 여러 개를 한 트랜잭션으로 저장 */
    List<GoalResponseDTO> saveAll(Long userId, List<GoalRequestDTO> dtos);

    List<GoalResponseDTO> findMine(Long userId, String yearMonth);
}
