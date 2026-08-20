package com.se1908.group01.dto;

import com.se1908.group01.enums.DocumentShareApprovalType;
import com.se1908.group01.enums.ShareApprovalStatus;
import java.time.Instant;

public record UserDocumentShareApprovalResponse(
        Long approvalId,
        Long documentId,
        String documentName,
        DocumentShareApprovalType shareType,
        ShareApprovalStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}