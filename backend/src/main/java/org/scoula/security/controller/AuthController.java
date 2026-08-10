package org.scoula.security.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
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

    /**
     * 로그아웃 - token_version을 올려서, 지금까지 발급된 모든 토큰(다른 탭/기기 포함)을
     * 만료 전이어도 다음 요청부터 무효 처리한다. (JWT는 무상태라 서버가 "로그아웃됨"을
     * 별도로 기억할 방법이 없어서, 이 값을 기준선으로 삼는 방식)
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal CustomUser user) {
        userMapper.incrementTokenVersion(user.getUserId());
        log.info("로그아웃 - userId={} token_version 증가", user.getUserId());
        return ResponseEntity.ok().build();
    }
}
