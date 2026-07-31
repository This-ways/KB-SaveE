package org.scoula.peerstat.mapper;

import org.apache.ibatis.annotations.Param;
import org.scoula.peerstat.dto.CategoryCompareDTO;

import java.util.List;

public interface PeerStatMapper {

    // 연령대 계산용 생년월일 조회 (user 테이블 직접 조회, "yyyy-MM-dd" 문자열로 반환)
    // 주의: user는 A팀 도메인이지만 별도 UserMapper를 새로 만들지 않고
    // 이 조회만 여기서 직접 함 (필요한 게 birth_date 한 컬럼뿐이라 도메인 침범 최소화)
    String getBirthDate(@Param("userId") Long userId);

    // 카테고리별 "내 소비 vs 연령별 평균 소비" Top3 (지출만, 내 소비 큰 순)
    List<CategoryCompareDTO> compareCategories(
            @Param("userId") Long userId,
            @Param("ageGroup") String ageGroup,
            @Param("yearMonth") String yearMonth
    );
}
