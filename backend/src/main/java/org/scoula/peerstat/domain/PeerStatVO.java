package org.scoula.peerstat.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// peer_stat 테이블 1행 (20대이하~60대이상 x 13개 카테고리, 수동 입력 마스터 데이터)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PeerStatVO {
    private Long peerStatId;
    private Long categoryId;
    private String ageGroup;   // "20대이하", "30대", "40대", "50대", "60대이상"
    private Integer avgAmount; // 해당 연령대의 카테고리별 평균 소비액
}
