package com.se1908.group01.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.se1908.group01.config.AiChatProperties;
import com.se1908.group01.enums.SupportedAiModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AiGenerationOptionsServiceTest {

	private AiGenerationOptionsService optionsService;

	@BeforeEach
	void setUp() {
		var properties = new AiChatProperties();
		properties.setDefaultModel("gpt-5.6-luna");
		properties.setDefaultTemperature(0.2);
		optionsService = new AiGenerationOptionsService(properties);
	}

	@Test
	void resolveUsesBackendDefaultsWhenRequestOptionsAreMissing() {
		var options = optionsService.resolve(null, null);

		assertEquals(SupportedAiModel.GPT_5_6_LUNA, options.model());
		assertEquals(0.2, options.temperature());
	}

	@Test
	void resolveAcceptsEverySupportedModel() {
		assertEquals(
				SupportedAiModel.GPT_5_6_LUNA,
				optionsService.resolve("gpt-5.6-luna", 0.0).model()
		);
		assertEquals(
				SupportedAiModel.GPT_5_6_TERRA,
				optionsService.resolve("gpt-5.6-terra", 0.5).model()
		);
		assertEquals(
				SupportedAiModel.GPT_5_6_SOL,
				optionsService.resolve("gpt-5.6-sol", 1.0).model()
		);
	}

	@Test
	void resolveRejectsUnsupportedModel() {
		assertThrows(
				IllegalArgumentException.class,
				() -> optionsService.resolve("gpt-5.6-cyber", 0.2)
		);
		assertThrows(
				IllegalArgumentException.class,
				() -> optionsService.resolve(" ", 0.2)
		);
	}

	@Test
	void resolveRejectsTemperatureOutsideAllowedRange() {
		assertThrows(
				IllegalArgumentException.class,
				() -> optionsService.resolve("gpt-5.6-luna", -0.1)
		);
		assertThrows(
				IllegalArgumentException.class,
				() -> optionsService.resolve("gpt-5.6-luna", 1.1)
		);
	}
}
