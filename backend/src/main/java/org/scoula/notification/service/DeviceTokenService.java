package org.scoula.notification.service;

import java.util.List;

public interface DeviceTokenService {
    void saveDeviceToken(Long userId, String fcmToken);
    List<String> getDeviceTokens(Long userId);
    void deleteAllByUserId(Long userId);
}