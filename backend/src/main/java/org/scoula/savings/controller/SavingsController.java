package org.scoula.savings.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.savings.dto.*;
import org.scoula.savings.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Log4j2
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

    @GetMapping("/products")
    public ResponseEntity<List<SavingsRecommendDTO>> getProductsByType(@RequestParam("type") String type) {
        // type 에는 "자유적금" 또는 "정액적금"이 들어옵니다.
        return ResponseEntity.ok(savingsRecommendService.getProductsByType(type));
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

    private final AutoTransferManageService manageService;

    // 자동이체 금액 수정 API
    @PutMapping(value = "/auto-transfer/{subscriptionId}/amount", produces = "application/json;charset=UTF-8")
    public ResponseEntity<String> updateMonthlyAmount(
            @PathVariable("subscriptionId") Long subscriptionId,
            @RequestBody UpdateMonthlyAmountReqDTO request) {
        try {
            // 서비스 메서드 시그니처(Long, Long)에 맞게 각각 값을 넘겨줌
            manageService.updateMonthlyAmount(subscriptionId, request.getNewAmount());

            return ResponseEntity.ok("자동이체 금액이 성공적으로 변경되었습니다.");
        } catch (IllegalArgumentException e) {
            log.warn("자동이체 금액 변경 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("자동이체 금액 변경 중 시스템 오류 발생", e);
            return ResponseEntity.internalServerError().body("서버 오류로 인해 처리에 실패했습니다.");
        }
    }


}

