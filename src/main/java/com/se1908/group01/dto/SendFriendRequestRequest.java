package com.se1908.group01.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendFriendRequestRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email is invalid")
    private String email;

}
