package com.se1908.group01.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdatePlanRequest {

    @NotBlank(message = "Plan name is required")
    @Size(max = 100, message = "Plan name must not exceed 100 characters")
    private String name;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", message = "Price must be greater than or equal to 0")
    private Double price;

    @NotNull(message = "Duration days is required")
    @Positive(message = "Duration days must be greater than 0")
    private Integer durationDays;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @NotNull(message = "Storage limit is required")
    @Positive(message = "Storage limit must be greater than 0")
    private Integer storageLimitGb;

    @NotBlank(message = "Allowed formats are required")
    @Size(max = 500, message = "Allowed formats must not exceed 500 characters")
    private String allowedFormats;

    @NotNull(message = "Maximum upload size is required")
    @Positive(message = "Maximum upload size must be greater than 0")
    private Integer maxUploadSizeMb;

    @NotNull(message = "Multiple documents setting is required")
    private Boolean multipleDocuments;

    @NotNull(message = "Video upload setting is required")
    private Boolean videoUpload;

    @NotNull(message = "Monthly token limit is required")
    @PositiveOrZero(message = "Monthly token limit must be greater than or equal to 0")
    private Long monthlyTokenLimit;
}
