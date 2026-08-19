package com.se1908.group01.repository;

import com.se1908.group01.entity.DocumentChunk;
import java.util.List;

import com.se1908.group01.enums.DocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Lưu và truy vấn các chunk được tạo bởi pipeline ingestion tài liệu bất đồng bộ.
 */
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

	// Xóa toàn bộ index cũ trước khi ingest/index lại một document.
	void deleteByDocumentDocumentId(Long documentId);

	// Single chat nạp các chunk của đúng một document theo thứ tự gốc.
	List<DocumentChunk> findByDocumentDocumentIdOrderByChunkIndexAsc(Long documentId);

	// Multi SelectedDocuments nạp chunk của danh sách ID đã được service kiểm tra quyền.
	@Query("SELECT dc FROM DocumentChunk dc JOIN FETCH dc.document WHERE dc.document.documentId IN :documentIds ORDER BY dc.document.documentId ASC, dc.chunkIndex ASC")
	List<DocumentChunk> findByDocumentIds(@Param("documentIds") List<Long> documentIds);

	// Nạp chunk READY mà user sở hữu hoặc document public khi search theo phạm vi accessible.
	@Query("SELECT dc FROM DocumentChunk dc JOIN FETCH dc.document WHERE dc.document.isDeleted = false AND dc.document.status = :status AND (dc.document.userId = :userId OR dc.document.isPublic = true) ORDER BY dc.document.documentId ASC, dc.chunkIndex ASC")
	List<DocumentChunk> findChunksByUserAccessible(@Param("userId") Long userId, @Param("status") DocumentStatus status);

	// Tương tự query trên nhưng giới hạn thêm document nằm trong folder cụ thể.
	@Query("SELECT dc FROM DocumentChunk dc JOIN FETCH dc.document WHERE dc.document.folderId = :folderId AND dc.document.isDeleted = false AND dc.document.status = :status AND (dc.document.userId = :userId OR dc.document.isPublic = true) ORDER BY dc.document.documentId ASC, dc.chunkIndex ASC")
	List<DocumentChunk> findChunksByUserAndFolderAccessible(@Param("userId") Long userId, @Param("folderId") Long folderId, @Param("status") DocumentStatus status);
}
