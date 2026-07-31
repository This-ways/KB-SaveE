package org.scoula.user.service;

import org.scoula.user.dto.DepositAccountResponseDTO;

public interface DepositAccountService {
    DepositAccountResponseDTO findMine(Long userId);
}
