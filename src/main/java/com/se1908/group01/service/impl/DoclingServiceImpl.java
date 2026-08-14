package com.se1908.group01.service.impl;

import com.se1908.group01.config.DoclingProperties;
import com.se1908.group01.dto.ChunkData;
import com.se1908.group01.dto.DoclingConvertResponse;
import com.se1908.group01.dto.DoclingChunkItem;
import com.se1908.group01.dto.DoclingChunkResponse;
import com.se1908.group01.exception.DoclingUnavailableException;
import com.se1908.group01.service.DoclingService;
import com.se1908.group01.service.MarkdownChunkingService;
import com.se1908.group01.util.FileExtensionUtil;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;

@Service
/**
 * Gọi Docling service tùy chọn để extract và chunk tài liệu có cấu trúc.
 * DOCX được convert qua Markdown; format khác dùng hybrid chunk endpoint.
 */
public class DoclingServiceImpl implements DoclingService {

	private static final Logger log = LoggerFactory.getLogger(DoclingServiceImpl.class);
	private static final String CHUNK_ENDPOINT = "/v1/chunk/hybrid/file";
	private static final String CONVERT_ENDPOINT = "/v1/convert/file";
	private static final int MAX_ATTEMPTS = 2;
	private static final int MAX_NUM_CHUNKS = 10000;
	private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
			"pdf",
			"docx",
			"pptx",
			"xlsx",
			"png",
			"jpg",
			"jpeg",
			"webp",
			"bmp",
			"tif",
			"tiff"
	);
	private static final Set<String> IMAGE_EXTENSIONS = Set.of(
			"png",
			"jpg",
			"jpeg",
			"webp",
			"bmp",
			"tif",
			"tiff"
	);
	private static final Map<String, String> DOCLING_FORMATS = Map.of(
			"pdf", "pdf",
			"docx", "docx",
			"pptx", "pptx",
			"xlsx", "xlsx"
	);

	private final RestClient restClient;
	private final DoclingProperties properties;
	private final MarkdownChunkingService markdownChunkingService;

	public DoclingServiceImpl(
			@Qualifier("doclingRestClient") RestClient restClient,
			DoclingProperties properties,
			MarkdownChunkingService markdownChunkingService
	) {
		this.restClient = restClient;
		this.properties = properties;
		this.markdownChunkingService = markdownChunkingService;
	}

	@Override
	public boolean supports(MultipartFile file) {
		// Chỉ chọn Docling khi service được bật và extension nằm trong tập được hỗ trợ.
		if (!properties.isEnabled() || file == null) {
			return false;
		}
		return SUPPORTED_EXTENSIONS.contains(
				FileExtensionUtil.getExtensionLower(file.getOriginalFilename())
		);
	}

	@Override
	public boolean isFallbackEnabled() {
		return properties.isFallbackEnabled();
	}

	@Override
	public List<ChunkData> chunk(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("Document file is required");
		}

		var extension = FileExtensionUtil.getExtensionLower(
				file.getOriginalFilename()
		);
		if (!SUPPORTED_EXTENSIONS.contains(extension)) {
			throw new IllegalArgumentException(
					"File format is not supported by Docling: " + extension
			);
		}

		DoclingUnavailableException lastUnavailableException = null;
		for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
			try {
				// Retry lỗi service tạm thời trước khi ingestion quyết định có fallback hay không.
				return requestChunks(file, extension);
			} catch (DoclingUnavailableException exception) {
				lastUnavailableException = exception;
				if (attempt < MAX_ATTEMPTS) {
					log.warn(
							"Docling request unavailable. Retrying attempt {}/{}",
							attempt + 1,
							MAX_ATTEMPTS
					);
				}
			}
		}

		throw lastUnavailableException == null
				? new DoclingUnavailableException("Docling service is unavailable")
				: lastUnavailableException;
	}

	private List<ChunkData> requestChunks(
			MultipartFile file,
			String extension
	) {
		if ("docx".equals(extension)) {
			return requestMarkdownChunks(file, extension);
		}
		return requestHybridChunks(file, extension);
	}

	private List<ChunkData> requestMarkdownChunks(
			MultipartFile file,
			String extension
	) {
		var body = new MultipartBodyBuilder();
		addFilePart(body, file);
		body.part("target_type", "inbody");
		body.part("from_formats", doclingFormat(extension));
		body.part("to_formats", "md");
		body.part("image_export_mode", "placeholder");
		body.part("do_ocr", Boolean.toString(shouldUseOcr(extension)));
		body.part("force_ocr", "false");
		body.part("table_mode", "accurate");
		body.part("do_table_structure", "true");
		body.part(
				"document_timeout",
				Integer.toString(Math.max(1, properties.getTimeoutSeconds()))
		);

		var response = executeRequest(
				CONVERT_ENDPOINT,
				body,
				DoclingConvertResponse.class
		);
		if (response == null
				|| response.document() == null
				|| !StringUtils.hasText(
						response.document().markdownContent()
				)) {
			throw new DoclingUnavailableException(
					"Docling returned no Markdown content"
			);
		}
		return markdownChunkingService.chunk(
				response.document().markdownContent(),
				properties.getMaxTokens()
		);
	}

	private List<ChunkData> requestHybridChunks(
			MultipartFile file,
			String extension
	) {
		var body = new MultipartBodyBuilder();
		addFilePart(body, file);
		body.part("include_converted_doc", "false");
		body.part("target_type", "inbody");
		body.part("convert_from_formats", doclingFormat(extension));
		body.part("convert_do_ocr", Boolean.toString(shouldUseOcr(extension)));
		body.part("convert_force_ocr", "false");
		body.part("convert_table_mode", "accurate");
		body.part("convert_do_table_structure", "true");
		body.part(
				"convert_document_timeout",
				Integer.toString(Math.max(1, properties.getTimeoutSeconds()))
		);
		body.part(
				"chunking_max_tokens",
				Integer.toString(Math.max(1, properties.getMaxTokens()))
		);
		body.part(
				"chunking_use_markdown_tables",
				Boolean.toString(properties.isUseMarkdownTables())
		);
		body.part(
				"chunking_merge_peers",
				Boolean.toString(properties.isMergePeers())
		);
		body.part("chunking_include_raw_text", "false");

		return mapChunks(executeRequest(
				CHUNK_ENDPOINT,
				body,
				DoclingChunkResponse.class
		));
	}

	private <T> T executeRequest(
			String endpoint,
			MultipartBodyBuilder body,
			Class<T> responseType
	) {
		try {
			return restClient.post()
					.uri(endpoint)
					.contentType(MediaType.MULTIPART_FORM_DATA)
					.headers(headers -> {
						if (StringUtils.hasText(properties.getApiKey())) {
							headers.set(
									"X-Api-Key",
									properties.getApiKey().trim()
							);
						}
					})
					.body(body.build())
					.retrieve()
					.body(responseType);
		} catch (ResourceAccessException exception) {
			throw new DoclingUnavailableException(
					"Could not connect to Docling service",
					exception
			);
		} catch (RestClientResponseException exception) {
			if (exception.getStatusCode().is5xxServerError()) {
				throw new DoclingUnavailableException(
						"Docling service returned "
								+ exception.getStatusCode().value(),
						exception
				);
			}
			throw new IllegalStateException(
					"Docling rejected the document request with status "
							+ exception.getStatusCode().value(),
					exception
			);
		}
	}

	private void addFilePart(
			MultipartBodyBuilder body,
			MultipartFile file
	) {
		Resource resource = file.getResource();
		var contentType = parseContentType(file.getContentType());
		body.part("files", resource)
				.filename(file.getOriginalFilename())
				.contentType(contentType);
	}

	private List<ChunkData> mapChunks(DoclingChunkResponse response) {
		if (response == null
				|| response.chunks() == null
				|| response.chunks().isEmpty()) {
			throw new DoclingUnavailableException(
					"Docling returned no document chunks"
			);
		}
		if (response.chunks().size() > MAX_NUM_CHUNKS) {
			throw new IllegalStateException(
					"Docling returned too many document chunks"
			);
		}

		var chunks = response.chunks()
				.stream()
				.filter(item -> StringUtils.hasText(item.text()))
				.sorted(Comparator.comparingInt(DoclingChunkItem::chunkIndex))
				.map(item -> new ChunkData(
						item.chunkIndex(),
						firstPageNumber(item.pageNumbers()),
						item.text().trim()
				))
				.toList();

		if (chunks.isEmpty()) {
			throw new DoclingUnavailableException(
					"Docling returned no text content"
			);
		}
		return chunks;
	}

	private Integer firstPageNumber(List<Integer> pageNumbers) {
		if (pageNumbers == null || pageNumbers.isEmpty()) {
			return null;
		}
		return pageNumbers.getFirst();
	}

	private String doclingFormat(String extension) {
		if (IMAGE_EXTENSIONS.contains(extension)) {
			return "image";
		}
		return DOCLING_FORMATS.get(extension);
	}

	private boolean shouldUseOcr(String extension) {
		return "pdf".equals(extension)
				|| IMAGE_EXTENSIONS.contains(extension);
	}

	private MediaType parseContentType(String contentType) {
		if (!StringUtils.hasText(contentType)) {
			return MediaType.APPLICATION_OCTET_STREAM;
		}
		try {
			return MediaType.parseMediaType(contentType);
		} catch (IllegalArgumentException exception) {
			return MediaType.APPLICATION_OCTET_STREAM;
		}
	}
}
