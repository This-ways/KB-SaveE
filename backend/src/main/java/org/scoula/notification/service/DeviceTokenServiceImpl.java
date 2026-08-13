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

    // 로그아웃 시 해당 유저의 모든 기기토큰을 지운다 - 남아 있으면 로그아웃 후에도 푸시가 계속 간다
    @Override
    public void deleteAllByUserId(Long userId) {
        deviceTokenMapper.deleteAllByUserId(userId);
        log.info("기기토큰 전체 삭제 - userId: {}", userId);
    }

}