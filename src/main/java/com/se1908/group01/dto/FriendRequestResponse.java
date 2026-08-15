package com.se1908.group01.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class FriendRequestResponse {

    private Long requestId;
    private Long senderId;
    private String senderName;
    private String senderEmail;
    private Long receiverId;
    private String receiverName;
    private String receiverEmail;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;

}
