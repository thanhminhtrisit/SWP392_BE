package com.se1908.group01.service;

import java.io.IOException;
import java.nio.file.Path;
import org.springframework.web.multipart.MultipartFile;

/**
 * Contract để giữ lại bytes upload và dispatch ingestion bất đồng bộ.
 */
public interface DocumentIngestionJobService {

	/** Copy bytes multipart thuộc request vào file tồn tại đến khi async ingestion chạy. */
	Path copyToTempFile(MultipartFile file) throws IOException;

	/** Parse và index tài liệu đã commit trong worker thread. */
	void ingestAsync(Long documentId, Path filePath, String originalFilename, String contentType);
}
