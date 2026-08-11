package org.scoula.report.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.report.dto.ReportDTO;
import org.scoula.report.service.ReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/report")
@Api(tags = "소비 리포트 API")
@Log4j2
@RequiredArgsConstructor
public class ReportController {

    private final ReportService service;

    // TODO: 인증(JWT) 구현 후에는 userId를 SecurityContext에서 꺼내도록 변경
    @GetMapping("")
    @ApiOperation(value = "월간 소비 리포트 종합 조회 (현금흐름/주차별/요일별/연령별비교/세이브/적금납입/AI요약)")
    public ReportDTO getReport(
            @RequestParam Long userId,
            @RequestParam String yearMonth
    ) {
        return service.getReport(userId, yearMonth);
    }

    @PostMapping("/ai-summary/refresh")
    @ApiOperation(value = "AI 요약 강제 재생성 (캐시 무시, 10분에 1번 제한)")
    public String refreshAiSummary(
            @RequestParam Long userId,
            @RequestParam String yearMonth
    ) {
        return service.refreshAiSummary(userId, yearMonth);
    }
}