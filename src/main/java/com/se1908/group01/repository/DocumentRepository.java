package com.se1908.group01.repository;

import com.se1908.group01.entity.Document;
import com.se1908.group01.entity.DocumentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

/**
 * Lưu metadata tài liệu và cung cấp query ownership/storage cho việc kiểm tra entitlement upload.
 */
public interface DocumentRepository extends JpaRepository<Document, Long>, JpaSpecificationExecutor<Document> {

	Optional<Document> findByDocumentIdAndUserId(Long documentId, Long userId);

	Optional<Document> findByDocumentIdAndUserIdAndIsDeletedFalse(Long documentId, Long userId);

	Optional<Document> findByDocumentIdAndIsPublicTrueAndIsDeletedFalse(Long documentId);

	List<Document> findByUserIdAndIsDeletedFalseOrderByUploadedAtDesc(Long userId);

	List<Document> findByUserIdAndIsStarredTrueAndIsDeletedFalseOrderByUploadedAtDesc(Long userId);

	List<Document> findByUserIdAndFolderIdAndIsDeletedFalseOrderByUploadedAtDesc(Long userId, Long folderId);

	List<Document> findByUserIdAndFolderIdOrderByUploadedAtDesc(Long userId, Long folderId);

	List<Document> findByUserIdAndFolderIdAndIsDeletedTrueAndDeletedAtGreaterThanEqualOrderByUploadedAtDesc(Long userId, Long folderId, java.time.Instant minDeletedAt);

	List<Document> findByIsPublicTrueAndIsDeletedFalseOrderByUploadedAtDesc();

	List<Document> findByUserIdAndIsDeletedTrueOrderByDeletedAtDesc(Long userId);

	boolean existsByUserIdAndOriginalFileNameAndFileSizeAndIsDeletedFalse(Long userId, String originalFileName, Long fileSize);

	@Query("""
			SELECT COALESCE(SUM(d.fileSize), 0)
			FROM Document d
			WHERE d.userId = :userId
			  AND d.isDeleted = false
			""")
	// Chỉ cộng tài liệu chưa bị xóa vì file soft-delete không còn tính vào quota storage đang dùng.
	long sumActiveStorageBytesByUserId(@Param("userId") Long userId);

	@Modifying
	@Query("update Document d set d.folderId = null where d.userId = :userId and d.folderId = :folderId")
	void clearFolderForUser(@Param("userId") Long userId, @Param("folderId") Long folderId);

	@Modifying
	@Query("UPDATE Document d SET d.isDeleted = true, d.deletedAt = :deletedAt WHERE d.userId = :userId AND d.folderId = :folderId AND d.isDeleted = false")
	void softDeleteFolderDocuments(@Param("userId") Long userId, @Param("folderId") Long folderId, @Param("deletedAt") java.time.Instant deletedAt);

	@Modifying
	@Query("UPDATE Document d SET d.isDeleted = false, d.deletedAt = null WHERE d.userId = :userId AND d.folderId = :folderId AND d.isDeleted = true")
	void restoreFolderDocuments(@Param("userId") Long userId, @Param("folderId") Long folderId);

	List<Document> findByUserIdAndFolderId(Long userId, Long folderId);

	@Query("SELECT d FROM Document d WHERE d.documentId IN :documentIds AND d.isDeleted = false AND d.status = :status AND (d.userId = :userId OR d.isPublic = true)")
	List<Document> findAccessibleDocumentsByIdsAndStatus(
			@Param("documentIds") List<Long> documentIds,
			@Param("userId") Long userId,
			@Param("status") DocumentStatus status);

	@Query("SELECT d FROM Document d WHERE d.isDeleted = false AND d.status = :status AND (d.userId = :userId OR d.isPublic = true)")
	// Toàn bộ tài liệu READY mà user có thể dùng: tài liệu sở hữu hoặc tài liệu public.
	List<Document> findAllAccessibleDocumentsByStatus(
			@Param("userId") Long userId,
			@Param("status") DocumentStatus status);

	@Query("SELECT d FROM Document d WHERE d.isDeleted = false AND d.status = :status AND d.userId = :userId")
	// Toàn bộ tài liệu READY thuộc user; không bao gồm public document của user khác.
	List<Document> findOwnedDocumentsByStatus(
			@Param("userId") Long userId,
			@Param("status") DocumentStatus status);

	@Query("SELECT d FROM Document d WHERE d.isDeleted = false AND d.status = :status AND d.userId = :userId AND d.folderId = :folderId")
	// Tài liệu READY thuộc user trong đúng folder được yêu cầu.
	List<Document> findOwnedDocumentsByFolderAndStatus(
			@Param("userId") Long userId,
			@Param("folderId") Long folderId,
			@Param("status") DocumentStatus status);

	@Query("SELECT d FROM Document d WHERE d.isDeleted = false AND d.status = :status AND ((d.userId = :userId AND d.folderId = :folderId) OR d.isPublic = true)")
	// Folder chỉ giới hạn tài liệu thuộc user; mọi tài liệu public READY vẫn được lấy.
	List<Document> findOwnedFolderAndPublicDocumentsByStatus(
			@Param("userId") Long userId,
			@Param("folderId") Long folderId,
			@Param("status") DocumentStatus status);
}
