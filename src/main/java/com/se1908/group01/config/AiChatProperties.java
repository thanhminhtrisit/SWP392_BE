package com.se1908.group01.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai")
public class AiChatProperties {

	// [SUA NGAY 2026-08-11 - co ho tro cua AI] Doi mac dinh Gemini -> OpenAI.
	// Gia tri nay chi duoc dung khi application.yaml khong khai app.ai.default-model.
	// Phai trung mot apiName trong enum SupportedAiModel, neu khong AiGenerationOptionsService
	// se nem IllegalArgumentException ngay khi co request chat dau tien.
	private String defaultModel = "gpt-5.6-luna";

	private double defaultTemperature = 0.2;

	public String getDefaultModel() {
		return defaultModel;
	}

	public void setDefaultModel(String defaultModel) {
		this.defaultModel = defaultModel;
	}

	public double getDefaultTemperature() {
		return defaultTemperature;
	}

	public void setDefaultTemperature(double defaultTemperature) {
		this.defaultTemperature = defaultTemperature;
	}
}
