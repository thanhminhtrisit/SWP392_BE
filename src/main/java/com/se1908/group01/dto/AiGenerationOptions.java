package com.se1908.group01.dto;

import com.se1908.group01.enums.SupportedAiModel;

public record AiGenerationOptions(SupportedAiModel model, double temperature) {

	public String modelName() {
		return model.getApiName();
	}
}
