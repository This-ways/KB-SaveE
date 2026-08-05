package org.scoula.notification.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.notification.domain.NotificationVO;
import org.scoula.notification.dto.NotificationResponseDTO;
import org.scoula.notification.service.NotificationService;
import org.scoula.security.account.domain.CustomUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // 알림함 조회 - targetMonth 있으면 해당 월만, 없으면 전체
    // 문구는 DB에 없으므로 DTO 변환 시점에 조립해서 내려줌
    @GetMapping
    public List<NotificationResponseDTO> getNotifications(@AuthenticationPrincipal CustomUser user,
                                                          @RequestParam(required = false) String targetMonth) {
        List<NotificationVO> list;

        if (targetMonth == null || targetMonth.isBlank()) {
            list = notificationService.getNotifications(user.getUserId());
        } else {
            list = notificationService.getNotificationsByMonth(user.getUserId(), targetMonth);
        }

        return list.stream()
                .map(NotificationResponseDTO::of)
                .collect(Collectors.toList());
    }

    // 특정 카테고리 예산 소진 체크 후 알림 발송 (화면 진입 시 프론트에서 호출)
    @PostMapping("/send")
    public void send(@AuthenticationPrincipal CustomUser user,
                     @RequestParam Long categoryId,
                     @RequestParam String targetMonth) {
        notificationService.checkAndNotify(user.getUserId(), categoryId, targetMonth);
    }

    // 목표를 설정한 전체 카테고리 소진 체크 (앱 진입 시 한 번 호출)
    // 카테고리별로 send 를 반복 호출하지 않도록 서버에서 대상 목록을 판단함
    @PostMapping("/check-all")
    public void checkAll(@AuthenticationPrincipal CustomUser user,
                         @RequestParam String targetMonth) {
        notificationService.checkAllCategories(user.getUserId(), targetMonth);
    }

    // 목표 변경 시 해당 월/카테고리 알림 삭제 (임계값 알림 재발송 허용)
    @DeleteMapping("/budget")
    public void deleteBudget(@AuthenticationPrincipal CustomUser user,
                             @RequestParam Long categoryId,
                             @RequestParam String targetMonth) {
        notificationService.deleteBudgetNotifications(user.getUserId(), categoryId, targetMonth);
    }

    // 오래된 알림 정리 (예산 설정 시 호출 - 강사님 피드백: 한 달 기준 유지)
    @DeleteMapping("/old")
    public void deleteOld(@AuthenticationPrincipal CustomUser user,
                          @RequestParam String currentMonth) {
        notificationService.deleteOldNotifications(user.getUserId(), currentMonth);
    }
}