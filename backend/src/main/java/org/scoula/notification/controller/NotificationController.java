package org.scoula.notification.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.notification.domain.NotificationVO;
import org.scoula.notification.service.NotificationService;
import org.scoula.security.account.domain.CustomUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Log4j2
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // 알림함 조회 - targetMonth 있으면 해당 월만, 없으면 전체
    @GetMapping
    public List<NotificationVO> getNotifications(@AuthenticationPrincipal CustomUser user,
                                                 @RequestParam(required = false) String targetMonth) {
        if (targetMonth == null || targetMonth.isBlank()) {
            return notificationService.getNotifications(user.getUserId());
        }
        return notificationService.getNotificationsByMonth(user.getUserId(), targetMonth);
    }

    // 예산 소진 체크 후 알림 발송 (거래 저장 시 지출분석 모듈이 호출)
    @PostMapping("/send")
    public void send(@AuthenticationPrincipal CustomUser user,
                     @RequestParam Long categoryId,
                     @RequestParam String targetMonth) {
        notificationService.checkAndNotify(user.getUserId(), categoryId, targetMonth);
    }

    // 목표 변경 시 해당 월/카테고리 알림 삭제 (임계값 알림 재발송 허용)
    @DeleteMapping("/budget")
    public void deleteBudget(@AuthenticationPrincipal CustomUser user,
                             @RequestParam Long categoryId,
                             @RequestParam String targetMonth) {
        notificationService.deleteBudgetNotifications(user.getUserId(), categoryId, targetMonth);
    }

    // 이전 달 알림 정리 (예산 설정 시 호출 - 강사님 피드백: 한 달 기준 유지)
    @DeleteMapping("/old")
    public void deleteOld(@AuthenticationPrincipal CustomUser user,
                          @RequestParam String currentMonth) {
        notificationService.deleteOldNotifications(user.getUserId(), currentMonth);
    }
}