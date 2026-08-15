package org.scoula.security.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtProcessor {
    static private final long TOKEN_VALID_MILISECOND = 1000L * 60 * 60 * 6; // 6시간
    private String secretKey = "충분히 긴 임의의(랜덤한) 비밀키 문자열 배정 ";
    private Key key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
//    private Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);  -- 운영시 사용

    // JWT 생성 - tokenVersion을 클레임으로 같이 심어서, 발급 이후 로그아웃(버전 증가)되면
    // 이 토큰은 만료 전이어도 다음 요청부터 무효 처리할 수 있게 한다.
    public String generateToken(String subject, int tokenVersion) {
        return Jwts.builder()
            .setSubject(subject)
                .claim("tokenVersion", tokenVersion)
                .setIssuedAt(new Date())
                .setExpiration(new Date(new Date().getTime() + TOKEN_VALID_MILISECOND))
                .signWith(key)
                .compact();
    }
// JWT Subject(username) 추출 - 해석 불가인 경우 예외 발생 // 예외 ExpiredJwtException, UnsupportedJwtException, MalformedJwtException, SignatureException,
// IllegalArgumentException
    public String getUsername(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    // 토큰 발급 시점에 심어둔 tokenVersion 추출 (로그인 이후 로그아웃 여부 판단용)
    public int getTokenVersion(String token) {
        Object v = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("tokenVersion");
        return v == null ? 0 : ((Number) v).intValue();
    }

    // JWT 검증(유효 기간 검증) - 해석 불가인 경우 예외 발생
    public boolean validateToken(String token) {
        Jws<Claims> claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
        return true;
    }


}
