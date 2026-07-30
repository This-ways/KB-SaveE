package org.scoula.transaction.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.transaction.domain.TransactionVO;
import org.scoula.transaction.dto.TransactionSummaryDTO;
import org.scoula.transaction.service.TransactionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@Api(tags = "거래내역 API")
@Log4j2
@RequiredArgsConstructor

public class TransactionController {

    private final TransactionService service;

    // TODO: 인증(JWT) 구현 후 userId는 SecurityContext에서 추출하도록 변경
    @GetMapping("")
    @ApiOperation(value = "월별 거래내역 목록 조회 (categoryId 없으면 전체)")
    public List<TransactionVO> getList(
            @RequestParam Long userId,
            @RequestParam String yearMonth,
            @RequestParam(required = false) Long categoryId
    ) {
        return service.getList(userId, yearMonth, categoryId);
    }
    @GetMapping("/summary")
    @ApiOperation(value = "월별 카테고리 Top5 + 나머지 요약")
    public TransactionSummaryDTO getSummary(
            @RequestParam Long userId,
            @RequestParam String yearMonth
    ) {
        return service.getSummary(userId, yearMonth);
    }
}
