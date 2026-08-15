package com.se1908.group01.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

/** Request cho nhánh stateless hỏi một câu về một document cụ thể. */
public class ChatAskRequest {

	@NotNull(message = "Document ID is required")
	private Long documentId;

	@NotBlank(message = "Question is required")
	private String question;

	private String model;

	@DecimalMin(value = "0.0", message = "Temperature must be between 0.0 and 1.0")
	@DecimalMax(value = "1.0", message = "Temperature must be between 0.0 and 1.0")
	private Double temperature;

	public Long getDocumentId() {
		return documentId;
	}

	public void setDocumentId(Long documentId) {
		this.documentId = documentId;
	}

	public String getQuestion() {
		return question;
	}

	public void setQuestion(String question) {
		this.question = question;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public Double getTemperature() {
		return temperature;
	}

	public void setTemperature(Double temperature) {
		this.temperature = temperature;
	}
}
