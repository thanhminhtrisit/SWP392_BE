package com.se1908.group01.repository;

import com.se1908.group01.entity.DocumentShareApproval;
import com.se1908.group01.enums.DocumentShareApprovalType;
import com.se1908.group01.enums.ShareApprovalStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentShareApprovalRepository extends JpaRepository<DocumentShareApproval, Long> {

	Optional<DocumentShareApproval> findByDocument_DocumentIdAndShareType(Long documentId, DocumentShareApprovalType shareType);

	List<DocumentShareApproval> findByDocument_DocumentId(Long documentId);

	void deleteByDocument_DocumentId(Long documentId);

	Page<DocumentShareApproval> findByStatus(ShareApprovalStatus status, Pageable pageable);

	Page<DocumentShareApproval> findByStatusAndShareType(
			ShareApprovalStatus status,
			DocumentShareApprovalType shareType,
			Pageable pageable
	);

	@Query(value = "SELECT a FROM DocumentShareApproval a " +
			"JOIN FETCH a.document d " +
			"WHERE d.userId = :userId " +
			"AND (:status IS NULL OR a.status = :status) " +
			"AND (:shareType IS NULL OR a.shareType = :shareType)",
			countQuery = "SELECT count(a) FROM DocumentShareApproval a " +
					"JOIN a.document d " +
					"WHERE d.userId = :userId " +
					"AND (:status IS NULL OR a.status = :status) " +
					"AND (:shareType IS NULL OR a.shareType = :shareType)")
	Page<DocumentShareApproval> findMyApprovalsWithFilters(
			@Param("userId") Long userId, // Thay kiểu dữ liệu cho khớp với user ID của bạn
			@Param("status") ShareApprovalStatus status,
			@Param("shareType") DocumentShareApprovalType shareType,
			Pageable pageable
	);

	List<DocumentShareApproval> findByDocument_UserId(Long userId);
}
