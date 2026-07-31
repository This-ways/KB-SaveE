package org.scoula.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.scoula.user.domain.DepositAccountVO;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepositAccountResponseDTO {
    private Long depositId;
    private int balance;

    public static DepositAccountResponseDTO of(DepositAccountVO vo) {
        return new DepositAccountResponseDTO(vo.getDepositId(), vo.getBalance());
    }
}
