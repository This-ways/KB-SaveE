package org.scoula.user.mapper;

import org.apache.ibatis.annotations.Param;
import org.scoula.user.domain.UserVO;

public interface UserMapper {
    int insert(UserVO vo);                 // 회원가입 (성공 시 vo.userId 채워짐)
    int existsByLoginId(String loginId);   // 중복 아이디 체크

    /** 마이데이터(계좌/카드) 연결 처리 - "연결하기" 버튼 눌렀을 때 호출 */
    int updateMydataConnected(@Param("userId") Long userId, @Param("connected") boolean connected);

    /** 평균 지출 계산 전, 연결 여부 확인용 */
    boolean findMydataConnected(@Param("userId") Long userId);
}
