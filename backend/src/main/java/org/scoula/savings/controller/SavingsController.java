package org.scoula.savings.controller;

import lombok.RequiredArgsConstructor;
import org.scoula.savings.dto.*;
import org.scoula.savings.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/savings")
@RequiredArgsConstructor
public class SavingsController {

    private final SavingsService savingsService;

    private final SavingsRecommendService savingsRecommendService;
    private final SavingsDetailService savingsDetailService;

    private final SavingsSubscriptionService savingsSubscriptionService;

    private final SavingsStatusService savingsStatusService;

    private final SavingsCancelService cancelService;

    @GetMapping("/api")
    public String testApi() { //http://localhost:8080/api/savings/test
        savingsService.printSavingsData();
        return "Console check";
    }

    // 1. 적금 추천 API
    @PostMapping("/recommend")
    public ResponseEntity<List<SavingsRecommendDTO>> recommendSavings(@RequestBody RecommendationRequestDTO request) {
        List<SavingsRecommendDTO> result = savingsRecommendService.getRecommendedSavings(request);
        return ResponseEntity.ok(result);
    }

    // 2. 적금 상품 상세 정보 API
    @GetMapping("/products/{productId}")
    public ResponseEntity<SavingsDetailDTO> getSavingsDetail(@PathVariable Long productId) {
        SavingsDetailDTO detail = savingsDetailService.getSavingsDetail(productId);
        return ResponseEntity.ok(detail);
    }

    // 3. 적금 가입 정보 확인 API (Output 1 시뮬레이션)
    @PostMapping("/subscribe/confirm")
    public ResponseEntity<SavingsConfirmResDTO> confirmSubscription(@RequestBody SavingsSubscribeReqDTO request) {
        SavingsConfirmResDTO confirmData = savingsSubscriptionService.confirmSubscription(request);
        return ResponseEntity.ok(confirmData);
    }

    // 4. 최종 적금 가입 처리 API (Output 2 실제 DB 저장)
    @PostMapping("/subscribe")
    public ResponseEntity<SavingsSubscribeResDTO> processSubscription(@RequestBody SavingsSubscribeReqDTO request) {
        SavingsSubscribeResDTO result = savingsSubscriptionService.processSubscription(request);
        return ResponseEntity.ok(result);
    }

    // 5. 적금 현황 API
    @GetMapping("/status/{subscriptionId}")
    public ResponseEntity<SavingsStatusResDTO> getSavingsStatus(@PathVariable Long subscriptionId) {
        SavingsStatusResDTO status = savingsStatusService.getSavingsStatus(subscriptionId);
        return ResponseEntity.ok(status);
    }

    // 6. 해지 예상 명세서 조회 (
    @GetMapping("/{subscriptionId}/cancel/confirm")
    public ResponseEntity<CancelPreviewResDTO> getCancelPreview(@PathVariable Long subscriptionId) {
        CancelPreviewResDTO response = cancelService.getCancelPreview(subscriptionId);
        return ResponseEntity.ok(response);
    }

    // 7. 실제 해지 처리
    @PostMapping("/{subscriptionId}/cancel")
    public ResponseEntity<String> cancelSubscription(@PathVariable Long subscriptionId) {
        cancelService.cancelSubscription(subscriptionId);
        return ResponseEntity.ok("ok.");
    }
}

