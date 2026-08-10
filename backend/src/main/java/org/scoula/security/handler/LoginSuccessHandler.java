package org.scoula.security.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.common.util.ClockService;
import org.scoula.security.account.domain.CustomUser;
import org.scoula.security.account.dto.AuthResultDTO;
import org.scoula.security.account.dto.UserInfoDTO;
import org.scoula.security.util.JsonResponse;
import org.scoula.security.util.JwtProcessor;
import org.scoula.user.mapper.GoalMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Log4j2
@Component
@RequiredArgsConstructor
public class LoginSuccessHandler implements AuthenticationSuccessHandler {
    private final JwtProcessor jwtProcessor;
    private final ClockService clockService;
    private final GoalMapper goalMapper; // hasGoals(온보딩 완료 여부) 판단용

    private AuthResultDTO makeAuthResult(CustomUser user) {
        String loginId = user.getUsername();
        boolean hasGoals = goalMapper.countAllByUser(user.getUserVO().getUserId()) > 0;
        // 토큰 생성 - 로그인 시점의 tokenVersion을 같이 심어서 발급 (로그아웃 무효화 판단 기준)
        String token = jwtProcessor.generateToken(loginId, user.getUserVO().getTokenVersion());
        // 토큰 + 사용자 기본 정보(joinMonth, isTrial, hasGoals 포함)를 묶어서 AuthResultDTO 구성
        return new AuthResultDTO(token, UserInfoDTO.of(user.getUserVO(), clockService.currentYearMonth(), hasGoals));
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                         Authentication authentication) throws IOException, ServletException {
        CustomUser user = (CustomUser) authentication.getPrincipal();
        AuthResultDTO result = makeAuthResult(user);
        JsonResponse.send(response, result);
    }
}
