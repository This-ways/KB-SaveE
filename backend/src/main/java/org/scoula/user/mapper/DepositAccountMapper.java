package org.scoula.user.mapper;

import org.scoula.user.domain.DepositAccountVO;

public interface DepositAccountMapper {
    int insert(DepositAccountVO vo);          // 회원가입 시 자동 생성
    DepositAccountVO findByUserId(Long userId);
}
