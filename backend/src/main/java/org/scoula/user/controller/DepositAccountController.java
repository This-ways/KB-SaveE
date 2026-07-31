package org.scoula.user.controller;

import lombok.RequiredArgsConstructor;
import org.scoula.security.account.domain.CustomUser;
import org.scoula.user.dto.DepositAccountResponseDTO;
import org.scoula.user.service.DepositAccountService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/deposit-account")
@RequiredArgsConstructor
public class DepositAccountController {

    private final DepositAccountService depositAccountService;

    @GetMapping
    public DepositAccountResponseDTO findMine(@AuthenticationPrincipal CustomUser user) {
        return depositAccountService.findMine(user.getUserId());
    }
}
