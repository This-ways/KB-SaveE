package org.scoula.notification.dto;

import lombok.Builder;
import lombok.Data;
import org.scoula.notification.domain.NotificationVO;
import org.scoula.notification.util.NotificationMessage;

// 알림함 조회 응답
// 문구를 DB에 저장하지 않는 구조라, 조회 시점에 NotificationMessage 에서 조립해 내려줌
// (프론트에 문구를 중복 관리하지 않기 위함)
@Data
@Builder
public class NotificationResponseDTO {

    private Long notificationId;
    private Long categoryId;      // 프론트 아이콘 분기용
    private String typeCode;
    private String title;
    private String body;
    private String sentAt;

    public static NotificationResponseDTO of(NotificationVO vo) {

        String title;
        String body;

        // 예산 알림은 카테고리+임계값으로, 적금 알림은 typeCode 로 문구 조회
        if ("BUDGET".equals(vo.getTypeCode())) {
            title = NotificationMessage.getTitle(vo.getCategoryId(), vo.getThresholdRate());
            body = NotificationMessage.getBody(vo.getCategoryId(), vo.getThresholdRate());
        } else {
            title = NotificationMessage.getPaymentTitle(vo.getTypeCode());
            body = NotificationMessage.getPaymentBody(vo.getTypeCode());
        }

        return NotificationResponseDTO.builder()
                .notificationId(vo.getNotificationId())
                .categoryId(vo.getCategoryId())
                .typeCode(vo.getTypeCode())
                .title(title)
                .body(body)
                .sentAt(vo.getSentAt())
                .build();
    }

}