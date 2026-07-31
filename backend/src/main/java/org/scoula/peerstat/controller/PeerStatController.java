package org.scoula.peerstat.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.peerstat.dto.CategoryCompareDTO;
import org.scoula.peerstat.service.PeerStatService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/peer-stat")
@Api(tags = "연령별 소비 비교 API")
@Log4j2
@RequiredArgsConstructor
public class PeerStatController {

    private final PeerStatService service;

    // TODO: 인증(JWT) 구현 후에는 userId를 SecurityContext에서 꺼내도록 변경.
    @GetMapping("/compare")
    @ApiOperation(value = "카테고리별 내 소비 vs 연령별 평균 소비 Top3 비교")
    public List<CategoryCompareDTO> compare(
            @RequestParam Long userId,
            @RequestParam String yearMonth
    ) {
        return service.compareCategories(userId, yearMonth);
    }
}
