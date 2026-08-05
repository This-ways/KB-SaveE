package org.scoula.notification.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.notification.domain.NotificationVO;
import org.scoula.notification.mapper.NotificationMapper;
import org.scoula.notification.util.NotificationMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Log4j2
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;
    private final DeviceTokenService deviceTokenService;

    // 알림 임계값 (낮은 것부터)
    private static final int[] THRESHOLDS = {50, 70, 90};

    // 예산 소진 체크 : 목표 조회 -> 지출 합계 -> 소진율 계산 -> 넘은 임계값 알림
    @Override
    @Transactional
    public void checkAndNotify(Long userId, Long categoryId, String targetMonth) {

        // 1. 목표 금액 (목표를 안 세운 카테고리는 알림 대상이 아님)
        Integer target = notificationMapper.selectGoalAmount(userId, categoryId, targetMonth);
        if (target == null || target == 0) {
            log.info("목표 없음 - 알림 스킵. userId: {}, categoryId: {}", userId, categoryId);
            return;
        }

        // 2. 이번 달 누적 지출
        int spent = notificationMapper.selectMonthlySpending(userId, categoryId, targetMonth);

        // 3. 소진율 (%)
        int rate = spent * 100 / target;
        log.info("소진율 - userId: {}, categoryId: {}, 지출: {}/{} = {}%",
                userId, categoryId, spent, target, rate);

        // 4. 넘은 임계값을 전부 DB에 기록 (알림함에는 모두 남김)
        //    한 번에 여러 단계를 넘긴 경우(예: 40% -> 90%) 푸시가 여러 개 뜨면 사용자가 피로하므로
        //    신규 기록된 것 중 가장 높은 임계값 하나만 푸시로 발송
        int highestNew = 0;
        for (int threshold : THRESHOLDS) {
            if (rate < threshold) {
                continue;
            }

            NotificationVO vo = NotificationVO.builder()
                    .userId(userId)
                    .categoryId(categoryId)
                    .typeCode("BUDGET")
                    .targetMonth(targetMonth)
                    .thresholdRate(threshold)
                    .build();

            // INSERT IGNORE - 이미 보낸 알림이면 0 반환되어 건너뜀
            if (notificationMapper.insert(vo) > 0) {
                highestNew = threshold;
            } else {
                log.info("이미 기록된 알림 - userId: {}, categoryId: {}, {}%",
                        userId, categoryId, threshold);
            }
        }

        // 5. 신규 기록이 없으면(모두 중복) 푸시 없음
        if (highestNew == 0) {
            return;
        }

        sendPushToUser(userId,
                NotificationMessage.getTitle(categoryId, highestNew),
                NotificationMessage.getBody(categoryId, highestNew));
    }

    // 목표를 설정한 모든 카테고리를 한 번에 체크 (앱 진입 시 호출)
    // 사용자가 선택한 카테고리만 goal에 들어있으므로 그 목록만 순회하면 됨
    @Override
    @Transactional
    public void checkAllCategories(Long userId, String targetMonth) {
        List<Long> categoryIds = notificationMapper.selectGoalCategoryIds(userId, targetMonth);

        if (categoryIds.isEmpty()) {
            log.info("설정된 목표 없음 - 전체 체크 스킵. userId: {}, {}", userId, targetMonth);
            return;
        }

        for (Long categoryId : categoryIds) {
            checkAndNotify(userId, categoryId, targetMonth);
        }
    }

    // 알림 기록 저장 + 푸시 발송 (적금 알림 등 단건 알림용)
    @Override
    @Transactional
    public boolean processNotification(NotificationVO vo, String title, String body) {

        // 1. 기록 저장 - UNIQUE 걸리면 0 (이미 발송한 알림이므로 중단)
        int inserted = notificationMapper.insert(vo);
        if (inserted == 0) {
            log.info("이미 발송된 알림 - userId: {}, type: {}", vo.getUserId(), vo.getTypeCode());
            return false;
        }

        sendPushToUser(vo.getUserId(), title, body);
        return true;
    }

    // 유저의 모든 기기로 푸시 발송 (푸시 거부/토큰 없음 체크 포함)
    private void sendPushToUser(Long userId, String title, String body) {

        // 발송될 문구 확인용 - 문구를 DB에 저장하지 않는 구조라 여기서만 확인 가능
        log.info("알림 문구 - [{}] {}", title, body);

        // 푸시 거부 유저는 기록만 남김 (알림함에서는 보임)
        Boolean pushEnabled = notificationMapper.selectPushEnabled(userId);
        if (pushEnabled == null || !pushEnabled) {
            log.info("푸시 수신 거부 - 기록만 저장. userId: {}", userId);
            return;
        }

        // 등록된 모든 기기로 발송 (멀티 디바이스)
        List<String> tokens = deviceTokenService.getDeviceTokens(userId);
        if (tokens.isEmpty()) {
            log.warn("등록된 기기토큰 없음 - userId: {}", userId);
            return;
        }

        for (String token : tokens) {
            sendPush(token, title, body);
        }
    }

    // FCM 푸시 발송 - 한 기기가 실패해도 나머지는 계속 보내야 하므로 예외를 던지지 않음
    private void sendPush(String token, String title, String body) {
        try {
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            log.info("푸시 발송 성공: {}", response);

        } catch (Exception e) {
            log.warn("푸시 발송 실패 - token: {}..., 사유: {}",
                    token.length() > 20 ? token.substring(0, 20) : token, e.getMessage());
        }
    }

    @Override
    public List<NotificationVO> getNotifications(Long userId) {
        return notificationMapper.selectByUserId(userId);
    }

    @Override
    public List<NotificationVO> getNotificationsByMonth(Long userId, String targetMonth) {
        return notificationMapper.selectByUserIdAndMonth(userId, targetMonth);
    }

    @Override
    @Transactional
    public void deleteBudgetNotifications(Long userId, Long categoryId, String targetMonth) {
        int deleted = notificationMapper.deleteByCategoryAndMonth(userId, categoryId, targetMonth);
        log.info("예산 알림 삭제 - userId: {}, categoryId: {}, {}건", userId, categoryId, deleted);
    }

    // 예산 설정 시점에 지난달 알림을 정리 - 알림함이 무한정 쌓이지 않도록 한 달치만 유지
    @Override
    @Transactional
    public void deleteOldNotifications(Long userId, String currentMonth) {
        int deleted = notificationMapper.deleteBeforeMonth(userId, currentMonth);
        log.info("이전 달 알림 정리 - userId: {}, 기준월: {}, {}건", userId, currentMonth, deleted);
    }
}