package com.se1908.group01.repository;

import com.se1908.group01.entity.DocumentShareLink;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentShareLinkRepository extends JpaRepository<DocumentShareLink, Long> {

	Optional<DocumentShareLink> findByTokenAndEnabledTrue(String token);

	Optional<DocumentShareLink> findFirstByDocument_DocumentIdAndOwnerIdAndEnabledTrueOrderByCreatedAtDesc(
			Long documentId,
			Long ownerId
	);

	void deleteByDocument_DocumentId(Long documentId);
}
