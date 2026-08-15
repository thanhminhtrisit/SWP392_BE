package com.se1908.group01.service;

import com.se1908.group01.entity.Document;
import java.util.List;
import org.jspecify.annotations.Nullable;

public interface DocumentAccessService {

	Document getReadyDocumentForChat(Long userId, Long documentId);

	List<Document> getReadyDocumentsForChat(Long userId, List<Long> documentIds);

	/**
	 * Lấy các tài liệu READY dùng cho chế độ UserStorage.
	 *
	 * @param userId user đang thực hiện chat
	 * @param folderId folder cần giới hạn; {@code null} nghĩa là toàn bộ kho của user
	 * @param includePublicDocuments {@code true} để lấy thêm tài liệu public,
	 *                               {@code false} để chỉ lấy tài liệu thuộc user
	 */
	List<Document> getAllReadyDocumentsForUser(
			Long userId,
			@Nullable Long folderId,
			boolean includePublicDocuments
	);
}
