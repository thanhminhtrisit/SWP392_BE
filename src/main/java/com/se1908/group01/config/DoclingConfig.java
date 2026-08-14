package com.se1908.group01.config;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(DoclingProperties.class)
public class DoclingConfig {

	@Bean
	RestClient doclingRestClient(DoclingProperties properties) {
		var timeout = Duration.ofSeconds(
				Math.max(1, properties.getTimeoutSeconds())
		);
		var httpClient = HttpClient.newBuilder()
				.version(HttpClient.Version.HTTP_1_1)
				.connectTimeout(timeout)
				.build();
		var requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(timeout);

		return RestClient.builder()
				.baseUrl(normalizeBaseUrl(properties.getBaseUrl()))
				.requestFactory(requestFactory)
				.build();
	}

	private String normalizeBaseUrl(String baseUrl) {
		if (!StringUtils.hasText(baseUrl)) {
			return "http://localhost:5001";
		}
		return baseUrl.trim().replaceAll("/+$", "");
	}
}
