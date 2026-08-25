package com.se1908.group01.repository;

import com.se1908.group01.entity.DocumentShare;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentShareRepository extends JpaRepository<DocumentShare, Long> {

	List<DocumentShare> findBySharedWithUser_UserIdOrderByCreatedAtDesc(Long sharedWithUserId);

	Optional<DocumentShare> findByDocument_DocumentIdAndSharedWithUser_UserId(Long documentId, Long sharedWithUserId);

	Optional<DocumentShare> findByDocument_DocumentIdAndOwnerIdAndSharedWithUser_UserId(
			Long documentId,
			Long ownerId,
			Long sharedWithUserId
	);

	List<DocumentShare> findByDocument_DocumentIdAndOwnerIdOrderByCreatedAtDesc(Long documentId, Long ownerId);

	void deleteByDocument_DocumentId(Long documentId);

	List<DocumentShare> findByOwnerId(Long ownerId);
}
