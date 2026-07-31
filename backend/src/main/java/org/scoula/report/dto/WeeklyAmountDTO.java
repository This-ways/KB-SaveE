package org.scoula.report.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 주차별 소비 바 차트 한 줄 (일요일 기준 달력 주차: 1일이 무슨 요일이든 그 주가 1주차)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyAmountDTO {
    private Integer weekNo;
    private Integer amount;
}
