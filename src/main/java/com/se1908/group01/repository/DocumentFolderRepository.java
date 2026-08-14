package com.se1908.group01.repository;

import com.se1908.group01.entity.DocumentFolder;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Tìm folder theo owner để thao tác sau upload không thể trỏ tới folder của user khác.
 */
public interface DocumentFolderRepository extends JpaRepository<DocumentFolder, Long> {

	List<DocumentFolder> findByUserIdAndIsDeletedFalseOrderByNameAsc(Long userId);

	List<DocumentFolder> findByUserIdAndIsStarredTrueAndIsDeletedFalseOrderByNameAsc(Long userId);

	List<DocumentFolder> findByUserIdAndIsDeletedTrueOrderByDeletedAtDesc(Long userId);

	Optional<DocumentFolder> findByFolderIdAndUserIdAndIsDeletedFalse(Long folderId, Long userId);

	Optional<DocumentFolder> findByFolderIdAndUserId(Long folderId, Long userId);

	boolean existsByUserIdAndNameIgnoreCaseAndIsDeletedFalse(Long userId, String name);

	boolean existsByUserIdAndNameIgnoreCaseAndFolderIdNotAndIsDeletedFalse(Long userId, String name, Long folderId);
}
