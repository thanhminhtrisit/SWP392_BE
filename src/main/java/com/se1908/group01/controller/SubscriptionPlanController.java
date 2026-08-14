package com.se1908.group01.controller;

import com.se1908.group01.dto.CreatePlanRequest;
import com.se1908.group01.dto.ApiResponse;
import com.se1908.group01.dto.SubscriptionPlanResponse;
import com.se1908.group01.dto.UpdatePlanRequest;
import com.se1908.group01.service.SubscriptionPlanService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subscription-plans")
@RequiredArgsConstructor
public class SubscriptionPlanController {

    private final SubscriptionPlanService service;

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<SubscriptionPlanResponse> create(
            @Valid @RequestBody CreatePlanRequest request) {
        return ApiResponse.success(
                "Create subscription plan successfully",
                service.create(request)
        );
    }

    @GetMapping
    public ApiResponse<List<SubscriptionPlanResponse>> getAll() {
        return ApiResponse.success(
                "Get subscription plans successfully",
                service.getAll()
        );
    }

    @GetMapping("/{id}")
    public ApiResponse<SubscriptionPlanResponse> getById(
            @PathVariable Long id) {
        return ApiResponse.success(
                "Get subscription plan successfully",
                service.getById(id)
        );
    }

    @PutMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<SubscriptionPlanResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePlanRequest request) {
        return ApiResponse.success(
                "Update subscription plan successfully",
                service.update(id, request)
        );
    }

    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<Void> delete(
            @PathVariable Long id) {
        service.delete(id);
        return ApiResponse.success(
                "Delete subscription plan successfully",
                null
        );
    }
}
