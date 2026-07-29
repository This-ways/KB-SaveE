package org.scoula.member.mapper;

import org.scoula.member.dto.ChangePasswordDTO;
import org.scoula.security.account.domain.AuthVO;
import org.scoula.security.account.domain.MemberVO;

public interface MemberMapper {
    MemberVO get(String username); //모든 정보 (join처리)
    MemberVO findByUsername(String username); // id 중복 체크시 사용
    int insert(MemberVO member);  // 회원 정보 추가
    int insertAuth(AuthVO auth); // 회원 권한 정보 추가

    int update(MemberVO member); //회원정보 수정
    int updatePassword(ChangePasswordDTO changePasswordDTO);


}

