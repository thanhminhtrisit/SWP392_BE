package com.se1908.group01.service;

import com.se1908.group01.dto.ResendOtpRequest;
import com.se1908.group01.dto.ResendOtpResponse;
import com.se1908.group01.dto.VerifyOtpRequest;
import com.se1908.group01.dto.VerifyOtpResponse;

public interface OtpService {

    String generateOtp();
    VerifyOtpResponse verifyOtp(VerifyOtpRequest request);
    ResendOtpResponse resendOtp(ResendOtpRequest request);


}
