package com.se1908.group01.dto;

import com.se1908.group01.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class GoogleLoginResponse {

    private String message;
    private String token;
    private Long userId;
    private String email;
    private Role role;
    private String fullName;
    private String refreshToken;

}
