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

    public static UserInfoDTO of(UserVO user, YearMonth currentYearMonth) {
        YearMonth joinYearMonth = user.getJoinYearMonth();
        return new UserInfoDTO(
                user.getUserId(),
                user.getLoginId(),
                user.getUserName(),
                joinYearMonth.toString(),
                joinYearMonth.equals(currentYearMonth)
        );
    }
}
