package org.scoula.user.service;

import lombok.RequiredArgsConstructor;
import org.scoula.user.domain.DepositAccountVO;
import org.scoula.user.dto.DepositAccountResponseDTO;
import org.scoula.user.mapper.DepositAccountMapper;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class DepositAccountServiceImpl implements DepositAccountService {

    private final DepositAccountMapper depositAccountMapper;

    @Override
    public DepositAccountResponseDTO findMine(Long userId) {
        DepositAccountVO vo = depositAccountMapper.findByUserId(userId);
        if (vo == null) {
            throw new NoSuchElementException("예금계좌가 없습니다.");
        }
        return DepositAccountResponseDTO.of(vo);
    }
}
