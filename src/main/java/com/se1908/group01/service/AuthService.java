package com.se1908.group01.service;

import com.se1908.group01.dto.*;

public interface AuthService {

    RegisterResponse register(RegisterRequest request);
    VerifyOtpResponse verifyOtp(VerifyOtpRequest request);
    GoogleLoginResponse loginWithGoogle(String email, String fullName);
    LoginResponse login(LoginRequest request);
    RefreshResponse refresh(String rawRefreshToken);
    void logout(String rawRefreshToken);
}
