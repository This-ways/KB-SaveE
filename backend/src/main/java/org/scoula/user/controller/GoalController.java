package org.scoula.user.controller;

import lombok.RequiredArgsConstructor;
import org.scoula.security.account.domain.CustomUser;
import org.scoula.user.dto.CategoryAverageDTO;
import org.scoula.user.dto.ExpectedSavingDTO;
import org.scoula.user.dto.GoalRequestDTO;
import org.scoula.user.dto.GoalResponseDTO;
import org.scoula.user.service.GoalService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;

    // userId는 요청 파라미터가 아니라 JWT 인증주체(@AuthenticationPrincipal)에서만 꺼낸다 (팀 확정 보안 원칙)
    @PostMapping
    public GoalResponseDTO save(@AuthenticationPrincipal CustomUser user,
                                 @RequestBody GoalRequestDTO dto) {
        return goalService.save(user.getUserId(), dto);
    }

    // "카테고리 여러 개 선택 → 금액 입력 → 한번에 확인" 화면용 배치 저장
    @PostMapping("/batch")
    public List<GoalResponseDTO> saveAll(@AuthenticationPrincipal CustomUser user,
                                          @RequestBody List<GoalRequestDTO> dtos) {
        return goalService.saveAll(user.getUserId(), dtos);
    }

    @GetMapping
    public List<GoalResponseDTO> findMine(@AuthenticationPrincipal CustomUser user,
                                           @RequestParam(required = false) String yearMonth) {
        return goalService.findMine(user.getUserId(), yearMonth);
    }

    // 카테고리 선택 화면용 - "내 평균 지출" 표시 (mydata_connected=false면 빈 배열)
    @GetMapping("/category-averages")
    public List<CategoryAverageDTO> categoryAverages(@AuthenticationPrincipal CustomUser user,
                                                       @RequestParam(defaultValue = "3") int months) {
        return goalService.getCategoryAverages(user.getUserId(), months);
    }

    // 완료 화면용 - 이번 달 GOAL 기준 예상 절약액
    @GetMapping("/expected-saving")
    public ExpectedSavingDTO expectedSaving(@AuthenticationPrincipal CustomUser user,
                                             @RequestParam(required = false) String yearMonth,
                                             @RequestParam(defaultValue = "3") int months) {
        return goalService.getExpectedSaving(user.getUserId(), yearMonth, months);
    }
}
