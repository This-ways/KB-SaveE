package org.scoula.user.controller;

import lombok.RequiredArgsConstructor;
import org.scoula.security.account.domain.CustomUser;
import org.scoula.user.dto.SignupRequestDTO;
import org.scoula.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    // "계좌 연결"/"카드 연결" 버튼 - userId는 JWT에서만 추출 (파라미터 미신뢰 원칙)
    @PostMapping("/mydata/connect")
    public ResponseEntity<Void> connectMydata(@AuthenticationPrincipal CustomUser user) {
        userService.connectMydata(user.getUserId());
        return ResponseEntity.ok().build();
    }
}