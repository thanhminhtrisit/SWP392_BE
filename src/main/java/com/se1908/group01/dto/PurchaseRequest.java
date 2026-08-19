package com.se1908.group01.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class PurchaseRequest {

    @NotNull(message = "Plan ID is required")
    @Positive(message = "Plan ID must be greater than 0")
    private Long planId;

    @NotBlank(message = "Payment method is required")
    @Pattern(
            regexp = "(?i)VNPAY",
            message = "Payment method must be VNPAY"
    )
    private String paymentMethod;
}
