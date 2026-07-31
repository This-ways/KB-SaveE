package org.scoula.user.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepositAccountVO {
    private Long depositId;
    private Long userId;
    private int balance;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
