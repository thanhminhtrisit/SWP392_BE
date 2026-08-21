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

    // [SUA NGAY 2026-08-20 - co ho tro cua AI] Them columnDefinition NVARCHAR(100).
    // Ten goi cuoc hien la ASCII (Free/Basic/Pro) nhung admin co the doi thanh tieng Viet
    // qua API quan tri. Migration V3 doi cot.
    //
    // Luu y: truong description ben duoi van la columnDefinition = "TEXT" - CO Y chua doi.
    // TEXT khong luu duoc Unicode, nhung doi no doi hoi sua ca entity lan du lieu seed nen
    // tach ra migration rieng.
    @Column(nullable = false, length = 100, columnDefinition = "NVARCHAR(100)")
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
