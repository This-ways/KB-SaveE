package org.scoula.user.controller;

import lombok.RequiredArgsConstructor;
import org.scoula.security.account.domain.CustomUser;
import org.scoula.user.dto.SignupRequestDTO;
import org.scoula.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<Long> signup(@RequestBody SignupRequestDTO dto) {
        Long userId = userService.signup(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(userId);
    }

    // 아이디 중복확인 버튼 - 회원가입 폼 제출 전 미리 확인 (로그인 불필요, 공개 엔드포인트)
    @GetMapping("/check-id")
    public ResponseEntity<Boolean> checkLoginId(@RequestParam("loginId") String loginId) {
        return ResponseEntity.ok(userService.isLoginIdAvailable(loginId));
    }

    // "계좌 연결"/"카드 연결" 버튼 - userId는 JWT에서만 추출 (파라미터 미신뢰 원칙)
    @PostMapping("/mydata/connect")
    public ResponseEntity<Void> connectMydata(@AuthenticationPrincipal CustomUser user) {
        userService.connectMydata(user.getUserId());
        return ResponseEntity.ok().build();
    }

    // 마이페이지 알림 설정 - 토글 초기값
    @GetMapping("/push-enabled")
    public ResponseEntity<Boolean> getPushEnabled(@AuthenticationPrincipal CustomUser user) {
        return ResponseEntity.ok(userService.findPushEnabled(user.getUserId()));
    }

    // 마이페이지 알림 설정 - 푸시 수신 여부 변경
    @PatchMapping("/push-enabled")
    public ResponseEntity<Void> updatePushEnabled(
            @AuthenticationPrincipal CustomUser user,
            @RequestParam boolean enabled) {
        userService.updatePushEnabled(user.getUserId(), enabled);
        return ResponseEntity.ok().build();
    }

}