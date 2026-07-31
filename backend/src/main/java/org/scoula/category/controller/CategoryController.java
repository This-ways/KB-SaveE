package org.scoula.category.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.category.domain.CategoryVO;
import org.scoula.category.service.CategoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@Api(tags = "카테고리 API")
@Log4j2
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService service;

    @GetMapping("")
    @ApiOperation(value = "카테고리 전체 목록 조회 (13종 고정)")
    public List<CategoryVO> getList() {
        return service.getList();
    }
}
