package org.scoula.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.notification.domain.DeviceTokenVO;
import org.scoula.notification.mapper.DeviceTokenMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Log4j2
@Service
@RequiredArgsConstructor
public class DeviceTokenServiceImpl implements DeviceTokenService {

    private final DeviceTokenMapper deviceTokenMapper;   // 이름도 Repository → Mapper로

    @Override
    public void saveDeviceToken(Long userId, String fcmToken) {
        DeviceTokenVO vo = DeviceTokenVO.builder()   // builder 스타일 (팀 방식)
                .userId(userId)
                .fcmToken(fcmToken)
                .build();
        deviceTokenMapper.insertOrUpdate(vo);
        log.info("기기토큰 저장 - userId: {}", userId);
    }

    @Override
    public List<String> getDeviceTokens(Long userId) {
        return deviceTokenMapper.selectTokensByUserId(userId);
    }

    @Override
    public void deleteDeviceToken(String fcmToken) {
        deviceTokenMapper.deleteByToken(fcmToken);
        log.info("기기토큰 삭제");
    }
}