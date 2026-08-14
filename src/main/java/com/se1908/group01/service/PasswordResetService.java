package com.se1908.group01.service;

import com.se1908.group01.dto.*;

public interface PasswordResetService {

    ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request);

    VerifyForgotPasswordOtpResponse verifyForgotPasswordOtp(
            VerifyForgotPasswordOtpRequest request
    );

    ResetPasswordResponse resetPassword(ResetPasswordRequest request);

}
