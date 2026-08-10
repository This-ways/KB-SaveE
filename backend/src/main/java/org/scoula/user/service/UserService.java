package org.scoula.user.service;

import org.scoula.user.dto.SignupRequestDTO;

public interface UserService {
    /** 회원가입 + DEPOSIT_ACCOUNT 자동 생성 (기본 초기 잔액 100만원) */
    Long signup(SignupRequestDTO dto);

    /** "계좌/카드 연결" 버튼 - 마이데이터 연결 상태를 true로 전환 (시연용 플래그) */
    void connectMydata(Long userId);

    /** 아이디 중복확인 버튼 - 사용 가능하면 true. 형식(4~20자) 검증도 여기서 같이 함 */
    boolean isLoginIdAvailable(String loginId);
}