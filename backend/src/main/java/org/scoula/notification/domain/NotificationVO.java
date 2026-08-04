package org.scoula.notification.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// notification 테이블 매핑
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationVO {
    private Long notificationId;     // 알림 ID (PK)
    private Long userId;             // 사용자 ID (FK)
    private Long categoryId;         // 카테고리 ID (FK, 예산 알림이 아니면 null)
    private String typeCode;         // 분류코드 - 현재는 BUDGET만 사용
    private String targetMonth;      // 기준월 YYYY-MM (예산 알림만)
    private Integer thresholdRate;   // 임계값 50/70/90 (예산 알림만)
    private String sentAt;           // 발송일시
}