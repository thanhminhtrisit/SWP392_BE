package com.se1908.group01.service;

import com.se1908.group01.entity.Document;
import java.io.IOException;
import org.springframework.web.multipart.MultipartFile;

/**
 * Contract để chuyển file đã upload thành các document chunk có embedding.
 */
public interface DocumentIngestionService {

	/** Tạo và lưu các chunk có thể tìm kiếm cho một tài liệu. */
	int ingest(Document document, MultipartFile file) throws IOException;
}
