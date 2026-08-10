package org.scoula.security.filter;

import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.security.account.domain.CustomUser;
import org.scoula.security.util.JwtProcessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Log4j2
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";   // 끝에 공백 있음

    private final JwtProcessor jwtProcessor;
    private final UserDetailsService userDetailsService;

    private Authentication getAuthentication(String token) {
        String username = jwtProcessor.getUsername(token);
        UserDetails princiapl = userDetailsService.loadUserByUsername(username); // 매 요청마다 DB에서 최신 정보를 새로 읽어옴 (token_version 포함)

        // 로그아웃 무효화 체크: 토큰 발급 시점의 tokenVersion과 DB의 현재 tokenVersion을 비교.
        // 다르다는 건 그 사이 (다른 탭/기기 포함) 로그아웃이 있었다는 뜻이므로, 만료 전이어도 무효 처리한다.
        int tokenVersionInJwt = jwtProcessor.getTokenVersion(token);
        int currentTokenVersion = ((CustomUser) princiapl).getUserVO().getTokenVersion();
        if (tokenVersionInJwt != currentTokenVersion) {
            throw new JwtException("로그아웃되어 더 이상 유효하지 않은 토큰입니다.");
        }

        log.info("princiapl = " + princiapl);

        return new UsernamePasswordAuthenticationToken(princiapl, null, princiapl.getAuthorities());
    }

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

        if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
            String token = bearerToken.substring(BEARER_PREFIX.length());
            try {
                // 토큰에서 사용자 정보 추출 및 Authentication 객체 구성 후 SecurityContext에 저장
                Authentication authentication = getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException e) {
                // 토큰이 만료·손상됐어도 여기서 요청을 막으면 안 된다. 회원가입·로그인처럼
                // 인증이 필요 없는 화면(permitAll)까지 "예전에 브라우저에 남아있던 죽은 토큰"
                // 때문에 401로 튕겨나가는 버그가 있었음 - 인증 없이(익명) 다음 필터로 넘겨서,
                // 실제로 인증이 필요한 엔드포인트만 SecurityConfig의 authorizeRequests 단계에서
                // 정상적으로 401/403 처리되도록 한다.
                log.debug("유효하지 않은 토큰 - 인증 없이 진행 (필요한 화면이면 이후 단계에서 걸러짐): " + e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        super.doFilter(request, response, filterChain); }
}


