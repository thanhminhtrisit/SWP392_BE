package com.se1908.group01.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DoclingConvertResponse(
		DoclingConvertedDocument document,
		String status,
		@JsonProperty("processing_time") Double processingTime
) {
}
