package com.se1908.group01.service.impl;

import com.se1908.group01.dto.ResendOtpRequest;
import com.se1908.group01.dto.ResendOtpResponse;
import com.se1908.group01.dto.VerifyOtpRequest;
import com.se1908.group01.dto.VerifyOtpResponse;
import com.se1908.group01.entity.OtpVerification;
import com.se1908.group01.entity.User;
import com.se1908.group01.enums.AccountStatus;
import com.se1908.group01.enums.VerificationType;
import com.se1908.group01.exception.ResourceNotFoundException;
import com.se1908.group01.repository.OtpVerificationRepository;
import com.se1908.group01.repository.UserRepository;
import com.se1908.group01.service.EmailService;
import com.se1908.group01.service.OtpService;
import com.se1908.group01.service.SubscriptionLifecycleService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private final UserRepository userRepository;
    private final OtpVerificationRepository otpVerificationRepository;
    private final EmailService emailService;
    private final SubscriptionLifecycleService subscriptionLifecycleService;

    public String generateOtp() {
        int otp = (int) (Math.random() * 900000) + 100000;
        return String.valueOf(otp);
    }


    @Transactional
    public VerifyOtpResponse verifyOtp(VerifyOtpRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        OtpVerification otpVerification = otpVerificationRepository
                .findTopByUserIdAndVerificationTypeOrderByCreatedAtDesc(
                        user.getUserId(),
                        VerificationType.REGISTER
                )
                .orElseThrow(() -> new ResourceNotFoundException("OTP not found"));

        if (otpVerification.getAttempts() >= 5) {
            throw new IllegalArgumentException(
                    "Too many failed attempts"
            );
        }

        if (otpVerification.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("OTP has expired");
        }

        if (!otpVerification.getOtpCode().equals(request.getOtp())) {
            otpVerification.setAttempts(
                    otpVerification.getAttempts() + 1
            );
            otpVerificationRepository.save(otpVerification);
            throw new IllegalArgumentException("Invalid OTP!");
        }

        user.setStatus(AccountStatus.ACTIVE);
        user.setVerifiedStatus(true);
        userRepository.save(user);
        subscriptionLifecycleService.getOrCreateActiveSubscription(user);
        otpVerificationRepository.delete(otpVerification);
        return new VerifyOtpResponse("OTP verified successfully. Account actived.");
    }

    @Transactional
    public ResendOtpResponse resendOtp(@NonNull ResendOtpRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (Boolean.TRUE.equals(user.isVerifiedStatus())) {
            throw new IllegalArgumentException("Account already verified");
        }

        otpVerificationRepository
                .deleteAllByUserIdAndVerificationType(
                        user.getUserId(),
                        VerificationType.REGISTER
                );

        String otpCode = generateOtp();

        OtpVerification otpVerification = OtpVerification.builder()
                .userId(user.getUserId())
                .otpCode(otpCode)
                .verificationType(VerificationType.REGISTER)
                .attempts(0)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        otpVerificationRepository.save(otpVerification);
        emailService.sendOtpEmail(user.getEmail(), otpCode);
        return new ResendOtpResponse(
                "OTP has been resent to your email.",
                user.getEmail()
        );

    }

}
