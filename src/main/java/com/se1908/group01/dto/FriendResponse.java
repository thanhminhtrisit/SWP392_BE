package com.se1908.group01.dto;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Data
@Builder
public class FriendResponse {

    private Long friendshipId;
    private Long userId;
    private String fullName;
    private String email;
    private LocalDateTime createdAt;

}