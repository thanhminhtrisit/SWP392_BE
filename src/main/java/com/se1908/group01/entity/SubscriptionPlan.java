package com.se1908.group01.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "subscription_plan")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Double price;

    @Column(nullable = false)
    private Integer durationDays;

    @Column(columnDefinition = "TEXT")
    private String description;

    // ================= STORAGE =================

    // Tổng dung lượng lưu trữ (GB)
    @Column(nullable = false)
    private Integer storageLimitGb;

    // Định dạng file được phép
    @Column(nullable = false, length = 500)
    private String allowedFormats;

    // Kích thước tối đa mỗi file (MB)
    @Column(nullable = false)
    private Integer maxUploadSizeMb;

    // Cho phép chat nhiều document
    @Column(nullable = false)
    private Boolean multipleDocuments;

    // Cho phép upload video
    @Column(nullable = false)
    private Boolean videoUpload;

    // ================= AI =================

    // Tổng token được sử dụng mỗi tháng
    @Column(nullable = false)
    private Long monthlyTokenLimit;

    // =======================================

    @Column(nullable = false)
    private boolean active;
}
