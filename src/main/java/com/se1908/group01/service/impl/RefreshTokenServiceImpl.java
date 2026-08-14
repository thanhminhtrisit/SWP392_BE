package com.se1908.group01.service.impl;

import com.se1908.group01.dto.RotationResult;
import com.se1908.group01.entity.RefreshToken;
import com.se1908.group01.repository.RefreshTokenRepository;
import com.se1908.group01.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    @Override
    @Transactional
    public String issue(Long userId, String ipAddress, String deviceInfo) {
        String rawToken = UUID.randomUUID().toString();
        String tokenHash = hash(rawToken);

        RefreshToken token = RefreshToken.builder()
                .userId(userId)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plus(refreshExpirationMs, ChronoUnit.MILLIS))
                .revoked(false)
                .ipAddress(ipAddress)
                .deviceInfo(deviceInfo)
                .build();

        refreshTokenRepository.save(token);
        return rawToken;
    }

    @Override
    @Transactional
    public RotationResult rotate(String rawToken) {
        String tokenHash = hash(rawToken);

        RefreshToken existing = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (existing.isRevoked()) {
            refreshTokenRepository.revokeAllByUserId(existing.getUserId());
            throw new IllegalArgumentException("Refresh token already used. All sessions have been revoked for security.");
        }

        if (existing.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(existing);
            throw new IllegalArgumentException("Refresh token expired. Please log in again.");
        }

        existing.setRevoked(true);
        refreshTokenRepository.save(existing);

        String newRawToken = issue(existing.getUserId(), existing.getIpAddress(), existing.getDeviceInfo());
        return new RotationResult(existing.getUserId(), newRawToken);
    }

    @Override
    @Transactional
    public void revoke(String rawToken) {
        String tokenHash = hash(rawToken);
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    @Override
    @Transactional
    public void revokeAll(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
