package com.se1908.group01.service.impl;

import com.se1908.group01.dto.*;
import com.se1908.group01.entity.OtpVerification;
import com.se1908.group01.entity.User;
import com.se1908.group01.enums.AuthProvider;
import com.se1908.group01.enums.VerificationType;
import com.se1908.group01.exception.ResourceNotFoundException;
import com.se1908.group01.repository.OtpVerificationRepository;
import com.se1908.group01.repository.UserRepository;
import com.se1908.group01.service.EmailService;
import com.se1908.group01.service.OtpService;
import com.se1908.group01.service.PasswordResetService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private final UserRepository userRepository;
    private final OtpVerificationRepository otpVerificationRepository;
    private final EmailService emailService;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!AuthProvider.LOCAL.equals(user.getProvider())) {
            throw new IllegalArgumentException("This account does not use password login");
        }

        otpVerificationRepository.deleteAllByUserIdAndVerificationType(
                user.getUserId(),
                VerificationType.FORGOT_PASSWORD
        );

        String otpCode = otpService.generateOtp();

        OtpVerification otpVerification = OtpVerification.builder()
                .userId(user.getUserId())
                .otpCode(otpCode)
                .verificationType(VerificationType.FORGOT_PASSWORD)
                .attempts(0)
                .verified(false)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        otpVerificationRepository.save(otpVerification);

        emailService.sendOtpEmail(user.getEmail(), otpCode);

        return new ForgotPasswordResponse(
                "OTP has been sent to your email.",
                user.getEmail()
        );
    }

    @Override
    public VerifyForgotPasswordOtpResponse verifyForgotPasswordOtp(
            VerifyForgotPasswordOtpRequest request
    ) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        OtpVerification otpVerification = otpVerificationRepository
                .findTopByUserIdAndVerificationTypeOrderByCreatedAtDesc(
                        user.getUserId(),
                        VerificationType.FORGOT_PASSWORD
                )
                .orElseThrow(() -> new ResourceNotFoundException("OTP not found"));

        if (otpVerification.getAttempts() >= 5) {
            throw new IllegalArgumentException("Too many faild attempts");
        }

        if (otpVerification.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("OTP has expired");
        }

        if (!otpVerification.getOtpCode().equals(request.getOtp())) {
            otpVerification.setAttempts(otpVerification.getAttempts() + 1);
            otpVerificationRepository.save(otpVerification);

            throw new IllegalArgumentException("Invalid OTP!");
        }

        otpVerification.setVerified(true);
        otpVerificationRepository.save(otpVerification);
        return new VerifyForgotPasswordOtpResponse(
                "OTP verified successfully. You can reset your password."
        );
    }

    @Override
    @Transactional
    public ResetPasswordResponse resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        OtpVerification otpVerification = otpVerificationRepository
                .findTopByUserIdAndVerificationTypeOrderByCreatedAtDesc(
                        user.getUserId(),
                        VerificationType.FORGOT_PASSWORD
                )
                .orElseThrow(() -> new ResourceNotFoundException("OTP verification required"));

        if (!otpVerification.isVerified()) {
            throw new IllegalArgumentException("OTP has not been verified");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        otpVerificationRepository.delete(otpVerification);

        return new ResetPasswordResponse("Password reset successfully.");
    }

}
