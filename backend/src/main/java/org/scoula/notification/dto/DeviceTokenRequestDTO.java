package org.scoula.notification.dto;

import lombok.Data;

// 기기토큰 저장 요청 (userId는 JWT에서 꺼내므로 fcmToken만 받음)
@Data
public class DeviceTokenRequestDTO {
    private String fcmToken;
}