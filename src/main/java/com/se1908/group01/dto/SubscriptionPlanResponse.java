package com.se1908.group01.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPlanResponse {

    private Long id;

    private String name;

    private Double price;

    private Integer durationDays;

    private String description;

    private Integer storageLimitGb;

    private String allowedFormats;

    private Integer maxUploadSizeMb;

    private Boolean multipleDocuments;

    private Boolean videoUpload;

    private Long monthlyTokenLimit;

    private boolean active;
}
