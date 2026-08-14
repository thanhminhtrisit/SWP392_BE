package com.se1908.group01.service.impl;

import com.se1908.group01.service.EmailService;
import lombok.AllArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;


@Service
@AllArgsConstructor
public class EmailServiceImpl implements EmailService {
    private final JavaMailSender mailSender;

    @Override
    public void sendOtpEmail(String toEmail, String otp) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(toEmail);
        message.setSubject("AI study Hub - OTP Verification");

        message.setText(
                "Your OTP code is: "
                + otp
                + "\n\nThis code will expires in 5 minutes."
        );

        mailSender.send(message);

    }



}
