package com.se1908.group01.security;

import com.se1908.group01.dto.GoogleLoginResponse;
import com.se1908.group01.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class OAuth2FrontendSuccessHandler implements AuthenticationSuccessHandler {

    private final ObjectProvider<AuthService> authServiceProvider;

    @Value("${app.frontend-base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        String email = principal.getAttribute("email");
        String fullName = principal.getAttribute("name");

        try {
            GoogleLoginResponse loginResponse = authServiceProvider.getObject().loginWithGoogle(email, fullName);
            response.sendRedirect(buildSuccessRedirectUrl(loginResponse));
        } catch (IllegalArgumentException ex) {
            response.sendRedirect(buildFailureRedirectUrl(ex.getMessage()));
        }
    }

    private String buildSuccessRedirectUrl(GoogleLoginResponse loginResponse) {
        return UriComponentsBuilder
                .fromUriString(normalizeFrontendBaseUrl() + "/oauth2/redirect")
                .queryParam("token", loginResponse.getToken())
                .queryParam("refreshToken", loginResponse.getRefreshToken())
                .queryParam("email", loginResponse.getEmail())
                .queryParam("userId", loginResponse.getUserId())
                .queryParam("role", loginResponse.getRole())
                .queryParam("fullName", loginResponse.getFullName())
                .build()
                .encode()
                .toUriString();
    }

    private String buildFailureRedirectUrl(String errorMessage) {
        return UriComponentsBuilder
                .fromUriString(normalizeFrontendBaseUrl() + "/login")
                .queryParam("error", errorMessage)
                .build()
                .encode()
                .toUriString();
    }

    private String normalizeFrontendBaseUrl() {
        return frontendBaseUrl.endsWith("/")
                ? frontendBaseUrl.substring(0, frontendBaseUrl.length() - 1)
                : frontendBaseUrl;
    }
}
