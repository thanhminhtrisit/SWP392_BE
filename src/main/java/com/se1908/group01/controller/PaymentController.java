package com.se1908.group01.controller;

import com.se1908.group01.dto.AdminPaymentListResponse;
import com.se1908.group01.dto.ApiResponse;
import com.se1908.group01.dto.PaymentCallbackResponse;
import com.se1908.group01.dto.PaymentHistoryResponse;
import com.se1908.group01.dto.PaymentPurchaseResponse;
import com.se1908.group01.dto.PurchaseRequest;
import com.se1908.group01.dto.RevenueResponse;
import com.se1908.group01.dto.SubscriptionResponse;
import com.se1908.group01.service.PaymentService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Value("${app.frontend-base-url:http://localhost:5173}")
    private String frontendBaseUrl;


    @PostMapping("/purchase")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<PaymentPurchaseResponse> purchase(
            Authentication authentication,
            @Valid @RequestBody PurchaseRequest request) {
        return ApiResponse.success(
                "Create payment successfully",
                paymentService.purchase(
                        authentication.getName(),
                        request
                )
        );
    }

    @GetMapping("/vnpay-return")
    public ResponseEntity<Void> vnPayReturn(
            @RequestParam Map<String, String> params) {
        PaymentCallbackResponse result =
                paymentService.handleVNPayCallback(params);

        URI redirectUri = UriComponentsBuilder
                .fromUriString(normalizedFrontendBaseUrl())
                .path("/payment-result")
                .queryParam("status", result.getStatus().name())
                .queryParam(
                        "transactionNo",
                        result.getTransactionNo()
                )
                .queryParam(
                        "alreadyProcessed",
                        result.isAlreadyProcessed()
                )
                .build()
                .encode()
                .toUri();

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(redirectUri)
                .build();
    }

    @GetMapping("/history")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<List<PaymentHistoryResponse>> history(
            Authentication authentication) {
        return ApiResponse.success(
                "Get payment history successfully",
                paymentService.getMyPaymentHistory(
                        authentication.getName()
                )
        );
    }

    @GetMapping("/revenue")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<RevenueResponse> revenue() {
        return ApiResponse.success(
                "Get payment revenue successfully",
                paymentService.getRevenue()
        );
    }

    @GetMapping
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<AdminPaymentListResponse> getAllPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {

        return ApiResponse.success(
                "Get all payments successfully",
                paymentService.getAllPayments(
                        status,
                        page,
                        size
                )
        );
    }

    @GetMapping("/my-subscription")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<SubscriptionResponse> mySubscription(
            Authentication authentication) {
        return ApiResponse.success(
                "Get my subscription successfully",
                paymentService.getMySubscription(
                        authentication.getName()
                )
        );
    }

    private String normalizedFrontendBaseUrl() {
        return frontendBaseUrl.endsWith("/")
                ? frontendBaseUrl.substring(
                        0,
                        frontendBaseUrl.length() - 1
                )
                : frontendBaseUrl;
    }
}
