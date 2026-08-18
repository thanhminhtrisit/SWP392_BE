package com.se1908.group01.dto;

import com.se1908.group01.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentPurchaseResponse {

    private Long paymentId;

    private String transactionNo;

    private String paymentUrl;

    private PaymentStatus status;
}
