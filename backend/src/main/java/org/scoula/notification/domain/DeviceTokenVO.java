package org.scoula.notification.domain;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceTokenVO {
    private Long tokenId;      // 토큰 ID (PK)
    private Long userId;       // 사용자 ID (FK)
    private String fcmToken;   // FCM 토큰 (UNIQUE)
    private String createdAt;  // 생성 시간
}