package com.se1908.group01.dto;

import java.util.List;

public class MultiChatAskResponse {

	private String answer;
	private String mode;
	private String model;
	private Double temperature;
	private List<Long> usedDocumentIds;
	private List<MultiChatSourceResponse> sources;

	public MultiChatAskResponse() {
	}

	public MultiChatAskResponse(
			String answer,
			String mode,
			String model,
			Double temperature,
			List<Long> usedDocumentIds,
			List<MultiChatSourceResponse> sources
	) {
		this.answer = answer;
		this.mode = mode;
		this.model = model;
		this.temperature = temperature;
		this.usedDocumentIds = usedDocumentIds;
		this.sources = sources;
	}

	public String getAnswer() {
		return answer;
	}

	public void setAnswer(String answer) {
		this.answer = answer;
	}

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
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

	public List<Long> getUsedDocumentIds() {
		return usedDocumentIds;
	}

	public void setUsedDocumentIds(List<Long> usedDocumentIds) {
		this.usedDocumentIds = usedDocumentIds;
	}

	public List<MultiChatSourceResponse> getSources() {
		return sources;
	}

	public void setSources(List<MultiChatSourceResponse> sources) {
		this.sources = sources;
	}
}
