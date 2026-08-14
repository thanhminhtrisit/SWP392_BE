package com.se1908.group01.service.impl;

import com.se1908.group01.config.DoclingProperties;
import com.se1908.group01.exception.DoclingUnavailableException;
import com.se1908.group01.service.MarkdownChunkingService;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DoclingServiceImplTest {

	private DoclingProperties properties;
	private MockRestServiceServer server;
	private DoclingServiceImpl service;

	@BeforeEach
	void setUp() {
		properties = new DoclingProperties();
		properties.setEnabled(true);
		properties.setBaseUrl("http://localhost:5001");
		properties.setMaxTokens(300);

		RestClient.Builder builder = RestClient.builder()
				.baseUrl(properties.getBaseUrl());
		server = MockRestServiceServer.bindTo(builder).build();
		service = new DoclingServiceImpl(
				builder.build(),
				properties,
				new MarkdownChunkingService()
		);
	}

	@Test
	void supportsOnlyEnabledDoclingFormats() {
		assertTrue(service.supports(file("sample.pdf")));
		assertTrue(service.supports(file("sample.docx")));
		assertTrue(service.supports(file("sample.png")));
		assertFalse(service.supports(file("legacy.doc")));
		assertFalse(service.supports(file("legacy.xls")));
		assertFalse(service.supports(file("video.mp4")));

		properties.setEnabled(false);

		assertFalse(service.supports(file("sample.pdf")));
	}

	@Test
	void chunkPreservesDocxHeadingHierarchyFromMarkdown() {
		server.expect(once(), requestTo(
						"http://localhost:5001/v1/convert/file"
				))
				.andExpect(method(HttpMethod.POST))
				.andRespond(withSuccess(
						"""
						{
						  "document": {
						    "filename": "sample.docx",
						    "md_content": "## I. Overview\\n\\n### 1. Introduction\\n\\n#### 1.1. First section\\n\\nFirst section body with enough text for a standalone semantic chunk.\\n\\n#### 1.2. Second section\\n\\nSecond section body with enough text for another standalone semantic chunk.\\n\\n##### 1. Human Actors\\n\\nHuman actors body with enough text for another standalone semantic chunk."
						  },
						  "status": "success",
						  "processing_time": 1.2
						}
						""",
						MediaType.APPLICATION_JSON
				));

		var chunks = service.chunk(file("sample.docx"));

		assertEquals(3, chunks.size());
		assertEquals(0, chunks.get(0).getChunkIndex());
		assertTrue(chunks.get(0).getContent().contains(
				"#### 1.1. First section"
		));
		assertTrue(chunks.get(1).getContent().contains(
				"#### 1.2. Second section"
		));
		assertTrue(chunks.get(2).getContent().contains(
				"##### 1. Human Actors"
		));
		assertTrue(chunks.get(2).getContent().contains(
				"#### 1.2. Second section"
		));
		server.verify();
	}

	@Test
	void chunkMapsHybridResponseForPdf() {
		server.expect(once(), requestTo(
						"http://localhost:5001/v1/chunk/hybrid/file"
				))
				.andExpect(method(HttpMethod.POST))
				.andRespond(withSuccess(
						"""
						{
						  "chunks": [
						    {
						      "filename": "sample.pdf",
						      "chunk_index": 0,
						      "text": "PDF chunk",
						      "num_tokens": 15,
						      "page_numbers": [2]
						    }
						  ],
						  "processing_time": 1.2
						}
						""",
						MediaType.APPLICATION_JSON
				));

		var chunks = service.chunk(file("sample.pdf"));

		assertEquals(1, chunks.size());
		assertEquals("PDF chunk", chunks.getFirst().getContent());
		assertEquals(2, chunks.getFirst().getPageNumber());
		server.verify();
	}

	@Test
	void chunkDoesNotRetryClientValidationError() {
		server.expect(once(), requestTo(
						"http://localhost:5001/v1/convert/file"
				))
				.andRespond(withStatus(
						HttpStatus.UNPROCESSABLE_ENTITY
				));

		assertThrows(
				IllegalStateException.class,
				() -> service.chunk(file("sample.docx"))
		);
		server.verify();
	}

	@Test
	void chunkRetriesServerErrorThenReportsUnavailable() {
		server.expect(once(), requestTo(
						"http://localhost:5001/v1/convert/file"
				))
				.andRespond(withStatus(
						HttpStatus.SERVICE_UNAVAILABLE
				));
		server.expect(once(), requestTo(
						"http://localhost:5001/v1/convert/file"
				))
				.andRespond(withStatus(
						HttpStatus.SERVICE_UNAVAILABLE
				));

		assertThrows(
				DoclingUnavailableException.class,
				() -> service.chunk(file("sample.docx"))
		);
		server.verify();
	}

	private MultipartFile file(String filename) {
		return new MockMultipartFile(
				"file",
				filename,
				"application/octet-stream",
				"test content".getBytes(StandardCharsets.UTF_8)
		);
	}
}
