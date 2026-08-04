package org.scoula.notification.service;

import org.scoula.notification.domain.NotificationVO;

import java.util.List;

public interface NotificationService {

    // 예산 소진 체크 후 알림 발송 (지출분석 모듈이 거래 저장 시 호출)
    void checkAndNotify(Long userId, Long categoryId, String targetMonth);

    // 알림 기록 저장 + 푸시 발송 (true: 신규 발송, false: 이미 보낸 알림)
    boolean processNotification(NotificationVO vo, String title, String body);

    // 알림함 조회
    List<NotificationVO> getNotifications(Long userId);

    // 특정 월 알림 조회
    List<NotificationVO> getNotificationsByMonth(Long userId, String targetMonth);

    // 목표 변경 시 해당 월/카테고리 알림 삭제 (임계값 알림 재발송 허용)
    void deleteBudgetNotifications(Long userId, Long categoryId, String targetMonth);

    // 이번 달 이전 알림 전체 삭제 (예산 설정 시 호출, 알림 무한 누적 방지)
    void deleteOldNotifications(Long userId, String currentMonth);
}