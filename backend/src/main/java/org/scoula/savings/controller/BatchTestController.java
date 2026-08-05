package org.scoula.savings.controller;

import lombok.RequiredArgsConstructor;
import org.scoula.savings.service.AutoTransferBatchService;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("dev")
@RestController
@RequestMapping("/api/test/batch")
@RequiredArgsConstructor
public class BatchTestController {

    private final AutoTransferBatchService batchService;

    // 1. 오전 9시 알림 테스트
    @PostMapping(value = "/notify", produces = "text/plain;charset=UTF-8")
    public String testNotify() {
        batchService.notifyUpcomingAutoTransfer();
        return "자동이체 알림 배치 실행 완료 (서버 로그 확인)";
    }

    // 2. 오전 10시 출금 테스트
    @PostMapping(value = "/execute", produces = "text/plain;charset=UTF-8")
    public String testExecute() {
        batchService.executeDailyAutoTransfer();
        return "자동이체 출금 배치 실행 완료 (서버 로그 및 DB 확인)";
    }
}