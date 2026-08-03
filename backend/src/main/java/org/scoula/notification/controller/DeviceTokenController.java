package org.scoula.notification.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.notification.dto.DeviceTokenRequestDTO;
import org.scoula.notification.service.DeviceTokenService;
import org.scoula.security.account.domain.CustomUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Log4j2
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class DeviceTokenController {

    private final DeviceTokenService deviceTokenService;

    // 기기토큰 저장 (userId는 JWT 인증주체에서, fcmToken만 body로)
    @PostMapping("/device-token")
    public void saveDeviceToken(@AuthenticationPrincipal CustomUser user,
                                @RequestBody DeviceTokenRequestDTO dto) {
        deviceTokenService.saveDeviceToken(user.getUserId(), dto.getFcmToken());
    }

    // 기기토큰 삭제 (로그아웃 시)
    @DeleteMapping("/device-token")
    public void deleteDeviceToken(@RequestParam String fcmToken) {
        deviceTokenService.deleteDeviceToken(fcmToken);
    }
}