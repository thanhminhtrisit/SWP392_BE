package com.se1908.group01.dto;

import com.se1908.group01.enums.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponse {

    // ===== Subscription =====
    private Long subscriptionId;

    private SubscriptionStatus status;

    private LocalDate startDate;

    private LocalDate endDate;

    // ===== Plan =====
    private String planName;

    private Double price;

    private Integer durationDays;

    // ===== Storage =====
    private Integer storageLimitGb;

    private String allowedFormats;

    private Integer maxUploadSizeMb;

    // ===== Features =====
    private Boolean multipleDocuments;

    private Boolean videoUpload;

    private Long monthlyTokenLimit;
}