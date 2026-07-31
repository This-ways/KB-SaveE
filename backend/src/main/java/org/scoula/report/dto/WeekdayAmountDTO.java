package org.scoula.report.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 요일별 소비 패턴 바 차트 한 줄 ("월"~"일" 고정 순서로 정렬돼서 내려옴)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WeekdayAmountDTO {
    private String dayOfWeek; // "월","화","수","목","금","토","일"
    private Integer amount;
}
