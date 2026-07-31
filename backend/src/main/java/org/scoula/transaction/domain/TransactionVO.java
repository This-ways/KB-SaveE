package org.scoula.transaction.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransactionVO {
    private Long txnId;
    private Long userId;
    private Long categoryId;      // 수입이거나 아낄 수 없는 고정지출(월세 등)이면 NULL
    private String categoryName;  // JOIN으로 채움 (categoryId가 NULL이면 이것도 NULL)
    private String merchantName;  // 가맹점명 or 거래내용 (수입이면 NULL)
    private Integer amount;       // 항상 양수로 저장/응답
    private String txnDate;       // "yyyy-MM-dd"
    private Boolean txnType;      // TRUE=수입, FALSE=지출
}
