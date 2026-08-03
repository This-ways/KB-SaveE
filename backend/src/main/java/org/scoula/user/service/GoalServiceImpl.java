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

        // 이번 달 목표가 아직 하나도 없으면(=서비스를 처음 열어본 경우) 지난달 목표를 먼저 이월시켜둔다.
        // 그래야 사용자가 손 안 댄 카테고리는 지난달 값 그대로 유지되고, 지금 저장하려는 카테고리만 덮어써짐.
        ensureCarriedOver(userId, yearMonth);

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
    @Transactional
    public List<GoalResponseDTO> findMine(Long userId, String yearMonth) {
        String target = (yearMonth == null || yearMonth.isBlank())
                ? clockService.currentYearMonth().toString()
                : yearMonth;

        // 조회 대상이 "이번 달"일 때만 이월 처리 - 과거 달 조회는 그때 기록 그대로 보여줘야 하므로 건드리지 않음
        if (target.equals(clockService.currentYearMonth().toString())) {
            ensureCarriedOver(userId, target);
        }

        return goalMapper.findByUserAndYearMonth(userId, target).stream()
                .map(GoalResponseDTO::of)
                .collect(Collectors.toList());
    }

    /**
     * targetYearMonth(보통 이번 달)에 목표가 하나도 없으면, "가장 최근 과거 달"의 목표를 그대로 복사해서 이월시킨다.
     * - 서비스를 몇 달 안 켜도(연속으로 건너뛰어도) 가장 마지막으로 설정했던 목표가 계속 이어지도록 함
     * - 이미 이번 달 목표가 하나라도 있으면(=이미 이월됐거나 사용자가 직접 설정함) 아무 것도 안 함
     * - 과거에 한 번도 목표를 설정한 적 없으면(트라이얼 등) 이월할 게 없어서 아무 일도 안 일어남
     */
    private void ensureCarriedOver(Long userId, String targetYearMonth) {
        List<GoalVO> current = goalMapper.findByUserAndYearMonth(userId, targetYearMonth);
        if (!current.isEmpty()) {
            return;
        }

        List<GoalVO> mostRecentPrior = goalMapper.findMostRecentPriorMonth(userId, targetYearMonth);
        if (mostRecentPrior.isEmpty()) {
            return;
        }

        for (GoalVO prev : mostRecentPrior) {
            GoalVO carried = GoalVO.builder()
                    .userId(userId)
                    .categoryId(prev.getCategoryId())
                    .yearMonth(targetYearMonth)
                    .targetAmount(prev.getTargetAmount())
                    .build();
            goalMapper.insert(carried);
        }
        log.debug("GOAL 이월 처리: userId={} {} -> {} ({}건)",
                userId, mostRecentPrior.get(0).getYearMonth(), targetYearMonth, mostRecentPrior.size());
    }