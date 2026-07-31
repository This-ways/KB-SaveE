package org.scoula.savings.controller;

import lombok.RequiredArgsConstructor;
import org.scoula.savings.dto.RecommendationRequestDTO;
import org.scoula.savings.dto.SavingsDetailDTO;
import org.scoula.savings.dto.SavingsRecommendDTO;
import org.scoula.savings.service.SavingsDetailService;
import org.scoula.savings.service.SavingsRecommendService;
import org.scoula.savings.service.SavingsService;
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
}

