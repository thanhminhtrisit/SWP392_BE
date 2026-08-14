package com.se1908.group01.config;

import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import java.io.IOException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties(GoogleSpeechProperties.class)
public class GoogleSpeechConfig {

    @Bean
    @ConditionalOnExpression(
            "T(org.springframework.util.StringUtils).hasText('${google.speech.gcs-bucket-name:}')"
    )
    SpeechClient speechClient() throws IOException {
        return SpeechClient.create();
    }

    @Bean
    @ConditionalOnExpression(
            "T(org.springframework.util.StringUtils).hasText('${google.speech.gcs-bucket-name:}')"
    )
    Storage googleCloudStorage() {
        return StorageOptions.getDefaultInstance().getService();
    }

    @Bean
    RestTemplate restTemplate() {
        return new RestTemplate();
    }
}