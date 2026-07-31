package org.scoula.security.account.mapper;

import org.scoula.user.domain.UserVO;

public interface UserDetailsMapper {
    /** status=10(정상) 회원만 조회 - 휴면/탈퇴 회원은 자동으로 로그인 차단됨 */
    UserVO get(String loginId);
}
