package org.scoula.security.account.domain;

import lombok.Getter;
import lombok.Setter;
import org.scoula.user.domain.UserVO;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;
import java.util.List;

@Getter
@Setter
public class CustomUser extends User {
    private UserVO userVO; // 실질적인 사용자 데이터

    private static final List<GrantedAuthority> FIXED_AUTHORITIES =
            List.of(new SimpleGrantedAuthority("ROLE_USER")); // 권한 테이블 없이 고정 ROLE_USER (팀 확정 사항)

    public CustomUser(String username, String password,
                      Collection<? extends GrantedAuthority> authorities) {
        super(username, password, authorities);
    }

    public CustomUser(UserVO vo) {
        super(vo.getLoginId(), vo.getPassword(), FIXED_AUTHORITIES);
        this.userVO = vo;
    }

    /** JWT 인증주체에서 userId를 꺼낼 때 사용 (파라미터 userId는 절대 신뢰하지 않는다는 팀 원칙) */
    public Long getUserId() {
        return userVO.getUserId();
    }
}
