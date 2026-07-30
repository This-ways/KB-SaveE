package org.scoula.peerstat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 소비 리포트 "Top3 카테고리 (내 소비 vs 연령별 평균 소비)"에서 쓰는 한 줄
// peer_stat에 해당 카테고리/연령대 조합이 없으면 peerAmount는 null일 수 있음
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryCompareDTO {
    private Long categoryId;
    private String categoryName;
    private Integer myAmount;
    private Integer peerAmount;
}
