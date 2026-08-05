package org.scoula.savings.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.savings.domain.SubscriptionVO;
import org.scoula.savings.mapper.SavingsMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@RequiredArgsConstructor
public class AutoTransferManageService { //납입액 수정

    private final SavingsMapper savingsMapper;

    @Transactional
    public void updateMonthlyAmount(Long subscriptionId, Long newAmount) {
        // 1. 가입 정보 및 상품 정보 조인 조회
        SubscriptionVO sub = savingsMapper.selectSubscriptionWithProduct(subscriptionId);
        if (sub == null) {
            throw new IllegalArgumentException("해당 가입 정보를 찾을 수 없습니다.");
        }

        // 2. 방어 로직: 자유적립식 여부 검증
        if (!"자유적립식".equals(sub.getUserSaveType())) {
            throw new IllegalArgumentException("정기적금은 자동이체 금액을 임의로 변경할 수 없습니다.");
        }

        // 3. 방어 로직: 최소 납입금액(min_amount) 검증
        if (sub.getMinAmount() != null && newAmount < sub.getMinAmount()) {
            throw new IllegalArgumentException(
                    String.format("변경할 금액은 상품의 최소 납입금액(%,d원) 이상이어야 합니다.", sub.getMinAmount())
            );
        }

        // 4. 방어 로직: 월 최대 납입 한도(max_amount) 초과 여부 검증
        if (sub.getMaxAmount() != null && sub.getMaxAmount() > 0) {
            if (newAmount > sub.getMaxAmount()) {
                throw new IllegalArgumentException(
                        String.format("변경할 금액이 월 최대 납입 한도(%,d원)를 초과할 수 없습니다.", sub.getMaxAmount())
                );
            }
        }

        // ==========================================
        // 검증 통과! 금액 수정 실행
        // ==========================================

        int result = savingsMapper.updateMonthlyAmount(subscriptionId, newAmount);

        if (result == 0) {
            throw new RuntimeException("금액 변경 처리에 실패했습니다.");
        }

        log.info("자동이체 금액 변경 성공 - Subscription ID: {}, 변경된 금액: {}", subscriptionId, newAmount);
    }
}