package com.se1908.group01.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DoclingChunkResponse(
		List<DoclingChunkItem> chunks,
		@JsonProperty("processing_time") Double processingTime
) {
}
