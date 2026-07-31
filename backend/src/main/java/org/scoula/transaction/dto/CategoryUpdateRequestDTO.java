package org.scoula.transaction.dto;

import lombok.Getter;
import lombok.Setter;

// PATCH /api/transactions/{txnId}/category 요청 body
// 예: { "categoryId": 2 }
@Getter
@Setter
public class CategoryUpdateRequestDTO {
    private Long categoryId;
}
