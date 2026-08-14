package com.se1908.group01.entity;

import com.se1908.group01.enums.VerificationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Entity
@Table (name = "otp_verification")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpVerification {

    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    @Column (name = "verification_id")
    private Long verificationId;

    @Column (name = "user_id", nullable = false)
    private Long userId;

    @Column (name = "otp_code", nullable = false)
    private String otpCode;

    @Column (name = "verification_type", nullable = false, length = 50)
    @Enumerated (EnumType.STRING)
    private VerificationType verificationType;

    @Column (nullable = false)
    private Integer attempts;

    @Column (name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column (name = "create_at", nullable = false)
    private LocalDateTime createdAt;

    @Column (name = "verified")
    private boolean verified;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (attempts == null) {
            attempts = 0;
        }

        verified = false;
    }

}
