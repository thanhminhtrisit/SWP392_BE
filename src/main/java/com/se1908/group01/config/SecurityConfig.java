package com.se1908.group01.config;

import com.se1908.group01.security.JwtAuthenticationFilter;
import com.se1908.group01.security.OAuth2FrontendFailureHandler;
import com.se1908.group01.security.OAuth2FrontendSuccessHandler;
import com.se1908.group01.security.RestAccessDeniedHandler;
import com.se1908.group01.security.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
/**
 * Cấu hình security dùng chung cho API.
 * /api/chat không nằm trong permitAll nên request chat phải mang JWT hợp lệ.
 */
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;
    private final OAuth2FrontendSuccessHandler oAuth2FrontendSuccessHandler;
    private final OAuth2FrontendFailureHandler oAuth2FrontendFailureHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {

        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .cors(cors ->
                        cors.configurationSource(
                                corsConfigurationSource()))

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS))

                .formLogin(form -> form.disable())

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/documents/share-link/*/save").authenticated()

                        // AUTH
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/auth/refresh",
                                "/api/auth/logout"
                        ).permitAll()

                        // OAUTH2
                        .requestMatchers(
                                "/oauth2/**",
                                "/login/oauth2/**"
                        ).permitAll()

                        // SWAGGER
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // PUBLIC DOCUMENT
                        .requestMatchers(
                                "/api/documents/public/**",
                                "/api/documents/share-link/**"
                        ).permitAll()

                        // SUBSCRIPTION PLANS
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/subscription-plans",
                                "/api/subscription-plans/**"
                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/subscription-plans"
                        ).hasRole("ADMIN")
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/subscription-plans/**"
                        ).hasRole("ADMIN")
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/subscription-plans/**"
                        ).hasRole("ADMIN")

                        // PAYMENT
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/payments/vnpay-return"
                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/payments",
                                "/api/payments/revenue"
                        ).hasRole("ADMIN")
                        .requestMatchers(
                                "/api/payments/**"
                        ).authenticated()

                        // ADMIN USER MANAGEMENT
                        .requestMatchers(
                                "/api/admin/users",
                                "/api/admin/users/**"
                        ).hasRole("ADMIN")

                        // Upload và các API tài liệu khác yêu cầu user đã xác thực bằng JWT, trừ các API được cho phép ở trên.
                        .anyRequest()
                        .authenticated()
                )

                .exceptionHandling(exception ->
                        exception
                                .authenticationEntryPoint(
                                        restAuthenticationEntryPoint)
                                .accessDeniedHandler(
                                        restAccessDeniedHandler))

                .oauth2Login(oauth2 ->
                        oauth2
                                .successHandler(
                                        oAuth2FrontendSuccessHandler)
                                .failureHandler(
                                        oAuth2FrontendFailureHandler))

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config =
                new CorsConfiguration();

        config.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:5173",
                "https://predefine-bribe-resurrect.ngrok-free.dev"
        ));

        config.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "DELETE",
                "PATCH",
                "OPTIONS"
        ));

        config.setAllowedHeaders(List.of("*"));

        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                config);

        return source;
    }
}
