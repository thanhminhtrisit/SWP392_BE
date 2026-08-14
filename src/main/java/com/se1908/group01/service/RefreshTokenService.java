package com.se1908.group01.service;

import com.se1908.group01.dto.RotationResult;

public interface RefreshTokenService {

    String issue(Long userId, String ipAddress, String deviceInfo);

    RotationResult rotate(String rawToken);

    void revoke(String rawToken);

    void revokeAll(Long userId);
}
