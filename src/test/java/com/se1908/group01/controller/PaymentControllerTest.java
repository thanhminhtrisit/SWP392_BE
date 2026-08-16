package com.se1908.group01.controller;

import com.se1908.group01.dto.PaymentCallbackResponse;
import com.se1908.group01.enums.PaymentStatus;
import com.se1908.group01.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URI;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @Test
    void vnPayReturnRedirectsToFrontendPaymentResult() {
        PaymentController controller =
                new PaymentController(paymentService);
        ReflectionTestUtils.setField(
                controller,
                "frontendBaseUrl",
                "http://localhost:5173/"
        );

        Map<String, String> params = Map.of(
                "vnp_TxnRef",
                "txn-001"
        );
        when(paymentService.handleVNPayCallback(params))
                .thenReturn(PaymentCallbackResponse.builder()
                        .transactionNo("txn-001")
                        .status(PaymentStatus.SUCCESS)
                        .alreadyProcessed(false)
                        .build());

        var response = controller.vnPayReturn(params);

        assertEquals(302, response.getStatusCode().value());
        assertEquals(
                URI.create(
                        "http://localhost:5173/payment-result"
                                + "?status=SUCCESS"
                                + "&transactionNo=txn-001"
                                + "&alreadyProcessed=false"
                ),
                response.getHeaders().getLocation()
        );
    }
}
