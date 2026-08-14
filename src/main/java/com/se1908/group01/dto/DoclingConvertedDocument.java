package com.se1908.group01.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DoclingConvertedDocument(
		String filename,
		@JsonProperty("md_content") String markdownContent
) {
}
