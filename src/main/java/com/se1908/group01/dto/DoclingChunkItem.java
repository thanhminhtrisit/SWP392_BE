package com.se1908.group01.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DoclingChunkItem(
		String filename,
		@JsonProperty("chunk_index") int chunkIndex,
		String text,
		@JsonProperty("raw_text") String rawText,
		@JsonProperty("num_tokens") Integer numTokens,
		List<String> headings,
		List<String> captions,
		@JsonProperty("page_numbers") List<Integer> pageNumbers,
		Map<String, Object> metadata
) {
}
