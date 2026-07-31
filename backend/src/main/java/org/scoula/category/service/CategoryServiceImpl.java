package org.scoula.category.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.category.domain.CategoryVO;
import org.scoula.category.mapper.CategoryMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Log4j2
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper mapper;

    @Override
    public List<CategoryVO> getList() {
        return mapper.getList();
    }
}
