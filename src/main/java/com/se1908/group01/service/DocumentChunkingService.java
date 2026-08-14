package com.se1908.group01.service;

import com.se1908.group01.dto.ChunkData;
import com.se1908.group01.dto.TextSegment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
/**
 * Chia text đã extract thành các chunk có giới hạn và giữ page metadata cho việc trích dẫn sau này.
 */
public class DocumentChunkingService {

	private static final String PAGE_NUMBER_METADATA = "pageNumber";
	private static final int CHUNK_SIZE_TOKENS = 300;
	private static final int MIN_CHUNK_SIZE_CHARS = 200;
	private static final int MIN_CHUNK_LENGTH_TO_EMBED = 5;
	private static final int MAX_NUM_CHUNKS = 10000;

	private final TokenTextSplitter textSplitter = TokenTextSplitter.builder()
			.withChunkSize(CHUNK_SIZE_TOKENS)
			.withMinChunkSizeChars(MIN_CHUNK_SIZE_CHARS)
			.withMinChunkLengthToEmbed(MIN_CHUNK_LENGTH_TO_EMBED)
			.withMaxNumChunks(MAX_NUM_CHUNKS)
			.withKeepSeparator(true)
			.build();

	public List<ChunkData> chunk(List<TextSegment> segments) {
		List<ChunkData> chunks = new ArrayList<>();
		int chunkIndex = 0;

		for (TextSegment segment : segments) {
			// Bỏ qua output rỗng để embedding provider chỉ nhận text có ý nghĩa.
			if (segment == null || !StringUtils.hasText(segment.getText())) {
				continue;
			}
			var text = segment.getText().trim();
			var pageNumber = segment.getPageNumber();
			var metadata = new HashMap<String, Object>();
			if (pageNumber != null) {
				metadata.put(PAGE_NUMBER_METADATA, pageNumber);
			}

			// Chia từng segment theo giới hạn token/character đã cấu hình và giữ lại số trang.
			for (Document splitDocument : textSplitter.split(Document.builder().text(text).metadata(metadata).build())) {
				var piece = splitDocument.getText();
				if (StringUtils.hasText(piece)) {
					chunks.add(new ChunkData(chunkIndex++, extractPageNumber(splitDocument.getMetadata()), piece));
				}
			}
		}

		return chunks;
	}

	private static Integer extractPageNumber(Map<String, Object> metadata) {
		var pageNumber = metadata.get(PAGE_NUMBER_METADATA);
		if (pageNumber instanceof Integer value) {
			return value;
		}
		if (pageNumber instanceof Number value) {
			return value.intValue();
		}
		return null;
	}
}
