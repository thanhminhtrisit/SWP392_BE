package com.se1908.group01.service;

import com.se1908.group01.dto.ChunkData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MarkdownChunkingService {

	private static final Pattern HEADING_PATTERN = Pattern.compile(
			"^(#{1,6})\\s+(.+?)\\s*$"
	);
	private static final int MIN_CHUNK_SIZE_CHARS = 50;
	private static final int MIN_CHUNK_LENGTH_TO_EMBED = 5;
	private static final int MAX_NUM_CHUNKS = 10000;

	public List<ChunkData> chunk(String markdown, int maxTokens) {
		if (!StringUtils.hasText(markdown)) {
			throw new IllegalArgumentException("Markdown content is required");
		}

		var splitter = TokenTextSplitter.builder()
				.withChunkSize(Math.max(1, maxTokens))
				.withMinChunkSizeChars(MIN_CHUNK_SIZE_CHARS)
				.withMinChunkLengthToEmbed(MIN_CHUNK_LENGTH_TO_EMBED)
				.withMaxNumChunks(MAX_NUM_CHUNKS)
				.withKeepSeparator(true)
				.build();
		var chunks = new ArrayList<ChunkData>();
		var chunkIndex = 0;

		for (MarkdownSection section : parseSections(markdown)) {
			var bodyDocument = Document.builder()
					.text(section.body())
					.build();
			for (Document splitDocument : splitter.split(bodyDocument)) {
				if (!StringUtils.hasText(splitDocument.getText())) {
					continue;
				}
				if (chunks.size() >= MAX_NUM_CHUNKS) {
					throw new IllegalStateException(
							"Markdown content produced too many chunks"
					);
				}
				chunks.add(new ChunkData(
						chunkIndex++,
						null,
						buildContent(section.headings(), splitDocument.getText())
				));
			}
		}

		if (chunks.isEmpty()) {
			throw new IllegalArgumentException(
					"Markdown content produced no chunks"
			);
		}
		return chunks;
	}

	private List<MarkdownSection> parseSections(String markdown) {
		var sections = new ArrayList<MarkdownSection>();
		var headingPath = new MarkdownHeading[6];
		var body = new StringBuilder();

		for (String line : markdown.replace("\r\n", "\n").split("\n", -1)) {
			var matcher = HEADING_PATTERN.matcher(line);
			if (!matcher.matches()) {
				body.append(line).append('\n');
				continue;
			}

			addSection(sections, headingPath, body);
			var level = matcher.group(1).length();
			headingPath[level - 1] = new MarkdownHeading(
					level,
					matcher.group(2).trim()
			);
			Arrays.fill(headingPath, level, headingPath.length, null);
		}
		addSection(sections, headingPath, body);
		return sections;
	}

	private void addSection(
			List<MarkdownSection> sections,
			MarkdownHeading[] headingPath,
			StringBuilder body
	) {
		var content = body.toString().trim();
		body.setLength(0);
		if (!StringUtils.hasText(content)) {
			return;
		}
		var headings = Arrays.stream(headingPath)
				.filter(heading -> heading != null)
				.toList();
		sections.add(new MarkdownSection(headings, content));
	}

	private String buildContent(
			List<MarkdownHeading> headings,
			String body
	) {
		var content = new StringBuilder();
		for (MarkdownHeading heading : headings) {
			content.append("#".repeat(heading.level()))
					.append(' ')
					.append(heading.title())
					.append('\n');
		}
		if (!headings.isEmpty()) {
			content.append('\n');
		}
		content.append(body.trim());
		return content.toString();
	}

	private record MarkdownHeading(int level, String title) {
	}

	private record MarkdownSection(
			List<MarkdownHeading> headings,
			String body
	) {
	}
}
