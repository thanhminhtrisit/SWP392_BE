package com.se1908.group01.repository;

import com.se1908.group01.entity.DocumentShareApproval;
import com.se1908.group01.enums.DocumentShareApprovalType;
import com.se1908.group01.enums.ShareApprovalStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentShareApprovalRepository extends JpaRepository<DocumentShareApproval, Long> {

	Optional<DocumentShareApproval> findByDocument_DocumentIdAndShareType(Long documentId, DocumentShareApprovalType shareType);

	List<DocumentShareApproval> findByDocument_DocumentId(Long documentId);

	boolean existsByDocument_DocumentIdAndShareTypeAndStatus(Long documentId, DocumentShareApprovalType shareType, ShareApprovalStatus status);

	void deleteByDocument_DocumentId(Long documentId);

	Page<DocumentShareApproval> findByStatus(ShareApprovalStatus status, Pageable pageable);

	Page<DocumentShareApproval> findByStatusAndShareType(
			ShareApprovalStatus status,
			DocumentShareApprovalType shareType,
			Pageable pageable
	);

	Page<DocumentShareApproval> findByDocument_UserId(Long userId, Pageable pageable);

	Page<DocumentShareApproval> findByDocument_UserIdAndStatus(Long userId, ShareApprovalStatus status, Pageable pageable);

	Page<DocumentShareApproval> findByDocument_UserIdAndStatusAndShareType(Long userId, ShareApprovalStatus status, DocumentShareApprovalType shareType, Pageable pageable);

	Page<DocumentShareApproval> findByDocument_UserIdAndShareType(Long userId, DocumentShareApprovalType shareType, Pageable pageable);
}
