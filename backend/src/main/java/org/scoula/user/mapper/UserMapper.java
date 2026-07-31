package org.scoula.user.mapper;

import org.scoula.user.domain.UserVO;

public interface UserMapper {
    int insert(UserVO vo);                 // 회원가입 (성공 시 vo.userId 채워짐)
    int existsByLoginId(String loginId);   // 중복 아이디 체크
}
