package com.se1908.group01.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class DocumentShareLinkResponse {

    private Long shareLinkId;
    private Long documentId;
    private String token;
    private String accessPath;
    private Boolean enabled;
    private Instant expiresAt;
    private Instant createdAt;


}
