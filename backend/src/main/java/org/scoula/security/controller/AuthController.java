package org.scoula.security.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.notification.service.DeviceTokenService;
import org.scoula.security.account.domain.CustomUser;
import org.scoula.user.mapper.UserMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Log4j2
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserMapper userMapper;
    private final DeviceTokenService deviceTokenService;

    /**
     * 로그아웃 - token_version을 올려서, 지금까지 발급된 모든 토큰(다른 탭/기기 포함)을
     * 만료 전이어도 다음 요청부터 무효 처리한다. (JWT는 무상태라 서버가 "로그아웃됨"을
     * 별도로 기억할 방법이 없어서, 이 값을 기준선으로 삼는 방식)
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal CustomUser user) {
        userMapper.incrementTokenVersion(user.getUserId());

        // 기기토큰도 함께 삭제 - 남아 있으면 로그아웃 후에도 이 기기로 푸시가 계속 간다
        // token_version 증가로 다른 기기도 같이 로그아웃되므로 user_id 기준 일괄 삭제
        deviceTokenService.deleteAllByUserId(user.getUserId());

        log.info("로그아웃 - userId={} token_version 증가, 기기토큰 삭제", user.getUserId());
        return ResponseEntity.ok().build();
    }
}