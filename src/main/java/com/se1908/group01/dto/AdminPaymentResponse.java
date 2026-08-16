package com.se1908.group01.dto;

import com.se1908.group01.enums.PaymentMethod;
import com.se1908.group01.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminPaymentResponse {

    private Long paymentId;

    private String transactionNo;

    private Long userId;

    private String userEmail;

    private Long planId;

    private String planName;

    private BigDecimal amount;

    private PaymentMethod paymentMethod;

    private PaymentStatus status;

    private String responseCode;

    private LocalDateTime createdAt;

    private LocalDateTime paidAt;
}
