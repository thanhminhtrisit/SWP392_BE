package com.se1908.group01.entity;

import com.se1908.group01.enums.AccountStatus;
import com.se1908.group01.enums.AuthProvider;
import com.se1908.group01.enums.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder

public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "full_name", nullable = false,  columnDefinition = "NVARCHAR(100)")
    private String fullName;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(nullable = false, length = 20)
    @Enumerated (EnumType.STRING)
    private AuthProvider provider;

    @Column(nullable = false, length = 20)
    @Enumerated (EnumType.STRING)
    private Role role;

    @Column(nullable = false, length = 20)
    @Enumerated (EnumType.STRING)
    private AccountStatus status;

    @Column(name = "verified_status", nullable = false)
    private boolean verifiedStatus;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createAt;

    // [SUA NGAY 2026-08-20 - co ho tro cua AI] Them columnDefinition NVARCHAR(500).
    // Tieu su do nguoi dung tu viet -> phai luu duoc tieng Viet. Migration V3 doi cot.
    // Truong fullName ben tren da la NVARCHAR(100) tu truoc, day la lam cho nhat quan.
    @Column(name = "bio", length = 500, columnDefinition = "NVARCHAR(500)")
    private String bio;

    @Column(name = "avatar_s3_key", length = 1024)
    private String avatarS3Key;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


    @PrePersist
    public void prePersist() {
        if (createAt == null) {
            createAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
