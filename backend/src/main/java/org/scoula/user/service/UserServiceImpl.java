package org.scoula.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.user.domain.DepositAccountVO;
import org.scoula.user.domain.UserVO;
import org.scoula.user.dto.SignupRequestDTO;
import org.scoula.user.mapper.DepositAccountMapper;
import org.scoula.user.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final int DEFAULT_INITIAL_BALANCE = 1_000_000;

    private final UserMapper userMapper;
    private final DepositAccountMapper depositAccountMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public Long signup(SignupRequestDTO dto) {
        if (userMapper.existsByLoginId(dto.getLoginId()) > 0) {
            throw new IllegalStateException("이미 사용 중인 아이디입니다.");
        }

        UserVO user = UserVO.builder()
                .loginId(dto.getLoginId())
                .password(passwordEncoder.encode(dto.getPassword()))
                .userName(dto.getUserName())
                .birthDate(dto.getBirthDate())
                .pushEnabled(true)
                .build();

        userMapper.insert(user); // useGeneratedKeys로 user.userId 채워짐

        // 회원가입 시 DEPOSIT_ACCOUNT 자동 생성
        DepositAccountVO deposit = DepositAccountVO.builder()
                .userId(user.getUserId())
                .balance(DEFAULT_INITIAL_BALANCE)
                .build();
        depositAccountMapper.insert(deposit);

        return user.getUserId();
    }
}
