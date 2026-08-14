package com.se1908.group01.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

class FileValidationServiceTest {

	private FileValidationService service;

	@BeforeEach
	void setUp() {
		service = new FileValidationService();
		ReflectionTestUtils.setField(service, "maxVideoFileSize", 50L * 1024L * 1024L);
		ReflectionTestUtils.setField(service, "maxAvatarFileSize", 5L * 1024L * 1024L);
	}

	@Test
	void acceptsSupportedDocumentImageAndVideo() {
		assertDoesNotThrow(() -> service.validateForUpload(file("sample.pdf", "application/pdf")));
		assertDoesNotThrow(() -> service.validateForUpload(file("sample.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")));
		assertDoesNotThrow(() -> service.validateForUpload(file("sample.jpg", "image/jpeg")));
		assertDoesNotThrow(() -> service.validateForUpload(file("sample.mp4", "video/mp4")));
	}

	@Test
	void rejectsNullOrEmptyFile() {
		assertThrows(IllegalArgumentException.class, () -> service.validateForUpload(null));
		assertThrows(
				IllegalArgumentException.class,
				() -> service.validateForUpload(new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]))
		);
	}

	@Test
	void rejectsUnsupportedExtension() {
		assertThrows(
				IllegalArgumentException.class,
				() -> service.validateForUpload(file("malware.exe", "application/octet-stream"))
		);
	}

	@Test
	void rejectsDocumentLargerThanTwentyMegabytes() {
		assertThrows(
				IllegalArgumentException.class,
				() -> service.validateForUpload(sizedFile(
						"large.pdf",
						"application/pdf",
						20L * 1024L * 1024L + 1
				))
		);
	}

	@Test
	void rejectsDocumentLargerThanSubscriptionLimit() {
		assertThrows(
				IllegalArgumentException.class,
				() -> service.validateForUpload(
						sizedFile(
								"large.pdf",
								"application/pdf",
								5L * 1024L * 1024L + 1
						),
						5
				)
		);
	}

	@Test
	void rejectsVideoLargerThanConfiguredLimit() {
		assertThrows(
				IllegalArgumentException.class,
				() -> service.validateForUpload(sizedFile(
						"large.mp4",
						"video/mp4",
						50L * 1024L * 1024L + 1
				))
		);
	}

	private MultipartFile file(String filename, String contentType) {
		return new MockMultipartFile(
				"file",
				filename,
				contentType,
				"content".getBytes(StandardCharsets.UTF_8)
		);
	}

	private MultipartFile sizedFile(String filename, String contentType, long size) {
		var file = mock(MultipartFile.class);
		when(file.isEmpty()).thenReturn(false);
		when(file.getSize()).thenReturn(size);
		when(file.getOriginalFilename()).thenReturn(filename);
		when(file.getContentType()).thenReturn(contentType);
		return file;
	}
}
