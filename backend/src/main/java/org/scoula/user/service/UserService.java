package org.scoula.user.service;

import org.scoula.user.dto.SignupRequestDTO;

public interface UserService {
    /** 회원가입 + DEPOSIT_ACCOUNT 자동 생성 (기본 초기 잔액 100만원) */
    Long signup(SignupRequestDTO dto);
}
