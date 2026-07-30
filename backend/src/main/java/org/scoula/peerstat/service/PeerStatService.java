package org.scoula.peerstat.service;

import org.scoula.peerstat.dto.CategoryCompareDTO;

import java.util.List;

public interface PeerStatService {

    // userId의 생년월일로 연령대를 계산한 뒤, 해당 월의 카테고리별 내 소비 vs 연령별 평균 소비 Top3 반환
    List<CategoryCompareDTO> compareCategories(Long userId, String yearMonth);
}
