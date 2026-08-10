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
    private static final int LOGIN_ID_MIN_LENGTH = 4;
    private static final int LOGIN_ID_MAX_LENGTH = 20; // 네이버(5~20)·카카오(6~20) 등 참고해서 4~20자로 결정
    private static final int PASSWORD_MIN_LENGTH = 4;
    private static final int PASSWORD_MAX_LENGTH = 20;

    private final UserMapper userMapper;
    private final DepositAccountMapper depositAccountMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public Long signup(SignupRequestDTO dto) {
        // 업무 로직 검증: DB에 그대로 밀어넣어서 DB 예외(Data truncation 등)로 터지게 하지 않고,
        // 여기서 먼저 걸러서 사용자에게 친절한 문구로 응답한다.
        if (dto.getLoginId() == null || dto.getLoginId().isBlank()) {
            throw new IllegalStateException("아이디를 입력해 주세요.");
        }
        if (dto.getLoginId().length() < LOGIN_ID_MIN_LENGTH || dto.getLoginId().length() > LOGIN_ID_MAX_LENGTH) {
            throw new IllegalStateException(
                    "아이디는 " + LOGIN_ID_MIN_LENGTH + "~" + LOGIN_ID_MAX_LENGTH + "자로 입력해 주세요.");
        }
        if (userMapper.existsByLoginId(dto.getLoginId()) > 0) {
            throw new IllegalStateException("이미 사용 중인 아이디입니다.");
        }
        if (dto.getPassword() == null
                || dto.getPassword().length() < PASSWORD_MIN_LENGTH
                || dto.getPassword().length() > PASSWORD_MAX_LENGTH) {
            throw new IllegalStateException(
                    "비밀번호는 " + PASSWORD_MIN_LENGTH + "~" + PASSWORD_MAX_LENGTH + "자로 입력해 주세요.");
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

    @Override
    @Transactional
    public void connectMydata(Long userId) {
        userMapper.updateMydataConnected(userId, true);
        log.debug("마이데이터 연결 처리 완료 userId={}", userId);
    }
}