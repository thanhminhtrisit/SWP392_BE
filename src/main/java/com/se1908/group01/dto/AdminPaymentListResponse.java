package com.se1908.group01.dto;

import java.util.List;

public record AdminPaymentListResponse(
        List<AdminPaymentResponse> payments,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
