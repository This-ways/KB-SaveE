package org.scoula.security.account.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.scoula.user.domain.UserVO;

import java.time.YearMonth;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoDTO {
    Long userId;
    String loginId;
    String userName;
    String joinMonth;   // "YYYY-MM"
    boolean isTrial;    // 가입월 == 현재월(currentYearMonth)이면 트라이얼
    boolean mydataConnected; // false면 로그인 직후 "데이터 연결해주세요" 화면부터, true면 메인으로 바로
    boolean hasGoals; // 지금까지(어느 달이든) 목표를 한 번이라도 설정한 적 있는지.
                       // mydataConnected=true인데 이게 false면 "계좌는 연결했지만 카테고리/예산
                       // 설정을 아직 안 끝낸" 상태 -> 로그인 시 /home이 아니라 /goal/category로
                       // 다시 보내서 온보딩을 이어가게 해야 한다.
    //pdf 비밀번호 구현
    String birthDate;  // "YYYY-MM-DD" - PDF 비밀번호(생년월일 6자리) 등에 활용

    public static UserInfoDTO of(UserVO user, YearMonth currentYearMonth, boolean hasGoals) {
        YearMonth joinYearMonth = user.getJoinYearMonth();
        return new UserInfoDTO(
                user.getUserId(),
                user.getLoginId(),
                user.getUserName(),
                joinYearMonth.toString(),
                joinYearMonth.equals(currentYearMonth),
                user.isMydataConnected(),
                hasGoals,
                //pdf 비밀번호 구현
                user.getBirthDate().toString()
        );
    }
}