package org.scoula.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.common.util.ClockService;
import org.scoula.user.domain.GoalVO;
import org.scoula.user.dto.GoalRequestDTO;
import org.scoula.user.dto.GoalResponseDTO;
import org.scoula.user.mapper.GoalMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
public class GoalServiceImpl implements GoalService {

    private final GoalMapper goalMapper;
    private final ClockService clockService;

    @Override
    @Transactional
    public GoalResponseDTO save(Long userId, GoalRequestDTO dto) {
        String yearMonth = (dto.getYearMonth() == null || dto.getYearMonth().isBlank())
                ? clockService.currentYearMonth().toString()
                : dto.getYearMonth();

        // 팀 확정 규칙: 카테고리별 목표는 해당 월 1~7일에만 설정/수정 가능
        if (!yearMonth.equals(clockService.currentYearMonth().toString())) {
            throw new IllegalStateException("이번 달 목표만 설정할 수 있습니다.");
        }
        if (!clockService.isWithinGoalEditWindow()) {
            throw new IllegalStateException("목표 설정은 매달 1~7일에만 가능합니다.");
        }

        GoalVO existing = goalMapper.findOne(userId, dto.getCategoryId(), yearMonth);

        GoalVO vo = GoalVO.builder()
                .goalId(existing != null ? existing.getGoalId() : null)
                .userId(userId)
                .categoryId(dto.getCategoryId())
                .yearMonth(yearMonth)
                .targetAmount(dto.getTargetAmount())
                .build();

        if (existing == null) {
            goalMapper.insert(vo); // UNIQUE 위반 시 DuplicateKeyException -> 409로 처리됨
        } else {
            goalMapper.update(vo);
        }

        return GoalResponseDTO.of(vo);
    }

    @Override
    @Transactional
    public List<GoalResponseDTO> saveAll(Long userId, List<GoalRequestDTO> dtos) {
        // 하나의 트랜잭션으로 묶어서 저장 - 중간에 하나라도 실패(중복 카테고리, 기간 위반 등)하면 전체 롤백
        return dtos.stream()
                .map(dto -> save(userId, dto))
                .collect(Collectors.toList());
    }

    @Override
    public List<GoalResponseDTO> findMine(Long userId, String yearMonth) {
        String target = (yearMonth == null || yearMonth.isBlank())
                ? clockService.currentYearMonth().toString()
                : yearMonth;

        return goalMapper.findByUserAndYearMonth(userId, target).stream()
                .map(GoalResponseDTO::of)
                .collect(Collectors.toList());
    }
}
