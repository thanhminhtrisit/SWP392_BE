package com.se1908.group01.dto;

import com.se1908.group01.enums.DocumentShareApprovalType;
import com.se1908.group01.enums.ShareApprovalStatus;
import java.time.Instant;

public record AdminDocumentShareApprovalResponse(
        Long approvalId,
        Long documentId,
        String documentName,
        Long ownerId,
        String ownerEmail,
        DocumentShareApprovalType shareType,
        ShareApprovalStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
