package com.se1908.group01.dto;

import java.util.List;

/** Response stateless gồm answer và metadata các chunk được dùng làm nguồn. */
public class ChatAskResponse {

	private Long documentId;
	private String answer;
	private String model;
	private Double temperature;
	private List<ChatSourceResponse> sources;

	public ChatAskResponse() {
	}

	public ChatAskResponse(
			Long documentId,
			String answer,
			String model,
			Double temperature,
			List<ChatSourceResponse> sources
	) {
		this.documentId = documentId;
		this.answer = answer;
		this.model = model;
		this.temperature = temperature;
		this.sources = sources;
	}

	public Long getDocumentId() {
		return documentId;
	}

	public void setDocumentId(Long documentId) {
		this.documentId = documentId;
	}

	public String getAnswer() {
		return answer;
	}

	public void setAnswer(String answer) {
		this.answer = answer;
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

	public List<ChatSourceResponse> getSources() {
		return sources;
	}

	public void setSources(List<ChatSourceResponse> sources) {
		this.sources = sources;
	}
}
