package org.scoula.user.service;

import org.scoula.user.dto.CategoryAverageDTO;
import org.scoula.user.dto.ExpectedSavingDTO;
import org.scoula.user.dto.GoalRequestDTO;
import org.scoula.user.dto.GoalResponseDTO;

import java.util.List;

public interface GoalService {
    /** 1~7일 수정제한 적용. 이미 있는 (user,category,month) 조합이면 update, 없으면 insert */
    GoalResponseDTO save(Long userId, GoalRequestDTO dto);

    /** "선택한 카테고리들 한번에 확인 후 제출" 화면용 - 여러 개를 한 트랜잭션으로 저장 */
    List<GoalResponseDTO> saveAll(Long userId, List<GoalRequestDTO> dtos);

    List<GoalResponseDTO> findMine(Long userId, String yearMonth);

    /** 카테고리 선택 화면용 - 카테고리별 최근 N개월 평균 지출. mydata_connected=false면 빈 리스트 */
    List<CategoryAverageDTO> getCategoryAverages(Long userId, int monthsBack);

    /** 완료 화면용 - 이번 달 설정된 GOAL 기준 예상 절약액 = Σ(평균 - 목표), 음수는 0 처리 */
    ExpectedSavingDTO getExpectedSaving(Long userId, String yearMonth, int monthsBack);
}
