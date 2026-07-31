package org.scoula.user.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class SignupRequestDTO {
    private String loginId;
    private String password;
    private String userName;
    private LocalDate birthDate;
}
