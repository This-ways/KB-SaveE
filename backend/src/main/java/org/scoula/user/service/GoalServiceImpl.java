package org.scoula.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.common.util.ClockService;
import org.scoula.notification.service.NotificationService;
import org.scoula.user.domain.GoalVO;
import org.scoula.user.dto.CategoryAverageDTO;
import org.scoula.user.dto.ExpectedSavingDTO;
import org.scoula.user.dto.GoalRequestDTO;
import org.scoula.user.dto.GoalResponseDTO;
import org.scoula.user.mapper.GoalMapper;
import org.scoula.user.mapper.TransactionStatMapper;
import org.scoula.user.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
public class GoalServiceImpl implements GoalService {

    private static final int DEFAULT_AVG_MONTHS = 3;

    private final GoalMapper goalMapper;
    private final ClockService clockService;
    private final UserMapper userMapper;
    private final TransactionStatMapper transactionStatMapper;
    private final NotificationService notificationService; // 목표 설정/이월 시점에 지난달 알림 정리 요청용

    @Override
    @Transactional
    public GoalResponseDTO save(Long userId, GoalRequestDTO dto) {
        // 단건 저장 API - "처음인지"를 이 호출 시점 기준으로 판단
        boolean isFirstTimeSetup = goalMapper.countAllByUser(userId) == 0;
        return save(userId, dto, isFirstTimeSetup);
    }

    /**
     * isFirstTimeSetup을 외부(saveAll)에서 미리 계산해 넘겨받는 내부용 버전.
     * saveAll에서 카테고리마다 이 메서드를 반복 호출하는데, 매번 새로 countAllByUser를 조회하면
     * 첫 카테고리가 저장된 순간 "처음"이 아니게 되어버려서 두 번째 카테고리부터 다시 1~7일 제한에
     * 걸리는 버그가 있었다. 그래서 배치 시작 전에 딱 한 번만 판단한 값을 그대로 써야 한다.
     */
    private GoalResponseDTO save(Long userId, GoalRequestDTO dto, boolean isFirstTimeSetup) {
        String yearMonth = (dto.getYearMonth() == null || dto.getYearMonth().isBlank())
                ? clockService.currentYearMonth().toString()
                : dto.getYearMonth();

        // 팀 확정 규칙: 카테고리별 목표는 해당 월 1~7일에만 설정/수정 가능
        // 단, 이 사용자가 지금까지 목표를 한 번도 설정한 적 없으면(=신규 가입자 최초 설정)
        // 날짜와 무관하게 허용한다. 안 그러면 8일 이후 가입한 신규 사용자는
        // 회원가입 -> 계좌연결 -> 카테고리 선택 온보딩 흐름 자체를 완주할 수 없다.
        if (!yearMonth.equals(clockService.currentYearMonth().toString())) {
            throw new IllegalStateException("이번 달 목표만 설정할 수 있습니다.");
        }
        if (!isFirstTimeSetup && !clockService.isWithinGoalEditWindow()) {
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

        // 예산 설정 시점 - 이번 달 이전 알림은 이제 철 지난 정보라 정리 요청
        notificationService.deleteOldNotifications(userId, yearMonth);

        return GoalResponseDTO.of(vo);
    }

    @Override
    @Transactional
    public List<GoalResponseDTO> saveAll(Long userId, List<GoalRequestDTO> dtos) {
        // "처음인지"는 배치 전체를 시작하기 전에 딱 한 번만 판단해서, 카테고리 여러 개를
        // 저장하는 동안 값이 중간에 바뀌지 않게 고정한다 (버그: 카테고리별로 매번 새로 판단하면
        // 첫 카테고리 저장 직후부터 "처음"이 아니게 되어 두 번째 카테고리부터 다시 막혔었음).
        boolean isFirstTimeSetup = goalMapper.countAllByUser(userId) == 0;

        // 하나의 트랜잭션으로 묶어서 저장 - 중간에 하나라도 실패(중복 카테고리, 기간 위반 등)하면 전체 롤백
        List<GoalResponseDTO> saved = dtos.stream()
                .map(dto -> save(userId, dto, isFirstTimeSetup))
                .collect(Collectors.toList());

        // 이 화면은 "이번 달 목표 전체를 다시 제출"하는 구조라, 이번에 빠진 카테고리는 해제된 것으로 보고 삭제한다.
        // (안 지우면 카테고리를 4개 -> 3개로 줄여도 예전 4번째 목표가 그대로 남음)
        if (!saved.isEmpty()) {
            String yearMonth = saved.get(0).getYearMonth();
            List<Long> keepCategoryIds = saved.stream()
                    .map(GoalResponseDTO::getCategoryId)
                    .collect(Collectors.toList());
            int removed = goalMapper.deleteNotIn(userId, yearMonth, keepCategoryIds);
            if (removed > 0) {
                log.debug("선택 해제된 카테고리 목표 삭제: userId={} yearMonth={} {}건", userId, yearMonth, removed);
            }
        }

        return saved;
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

    @Override
    public List<CategoryAverageDTO> getCategoryAverages(Long userId, int monthsBack) {
        // 마이데이터 미연결(false) 상태면 실제 거래내역이 있어도 아직 안 보여줘야 함 (연결 전/후 시나리오)
        if (!Boolean.TRUE.equals(userMapper.findMydataConnected(userId))) {
            return Collections.emptyList();
        }
        int months = monthsBack > 0 ? monthsBack : DEFAULT_AVG_MONTHS;
        String referenceYearMonth = clockService.currentYearMonth().toString();
        return transactionStatMapper.findCategoryAverages(userId, referenceYearMonth, months);
    }

    @Override
    public ExpectedSavingDTO getExpectedSaving(Long userId, String yearMonth, int monthsBack) {
        String target = (yearMonth == null || yearMonth.isBlank())
                ? clockService.currentYearMonth().toString()
                : yearMonth;

        List<CategoryAverageDTO> averages = getCategoryAverages(userId, monthsBack);
        if (averages.isEmpty()) {
            return new ExpectedSavingDTO(0);
        }

        List<GoalVO> goals = goalMapper.findByUserAndYearMonth(userId, target);
        int total = 0;
        for (GoalVO goal : goals) {
            for (CategoryAverageDTO avg : averages) {
                if (avg.getCategoryId().equals(goal.getCategoryId())) {
                    int diff = avg.getAvgAmount() - goal.getTargetAmount();
                    total += Math.max(diff, 0); // 평균보다 목표를 더 높게 잡은 경우 0으로 처리
                    break;
                }
            }
        }
        return new ExpectedSavingDTO(total);
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

        // 이월도 "목표가 새로 확정되는 시점"이라 동일하게 지난달 알림 정리
        notificationService.deleteOldNotifications(userId, targetYearMonth);
    }
}