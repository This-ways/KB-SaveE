package org.scoula.category.mapper;

import org.scoula.category.domain.CategoryVO;

import java.util.List;

public interface CategoryMapper {

    // 카테고리 전체 목록 조회 (13종 고정)
    List<CategoryVO> getList();
}
