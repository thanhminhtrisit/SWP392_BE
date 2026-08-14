package com.se1908.group01.repository;

import com.se1908.group01.entity.DocumentTag;
import com.se1908.group01.entity.DocumentTagId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository cho các liên kết nhiều-nhiều document/tag được tạo sau upload.
 */
public interface DocumentTagRepository extends JpaRepository<DocumentTag, DocumentTagId> {

	List<DocumentTag> findByDocumentDocumentIdOrderByTagNameAsc(Long documentId);

	boolean existsByDocumentDocumentIdAndTagTagId(Long documentId, Long tagId);

	void deleteByDocumentDocumentIdAndTagTagId(Long documentId, Long tagId);

	void deleteByDocumentDocumentId(Long documentId);

	void deleteByTagTagIdAndTagUserId(Long tagId, Long userId);

	@Query("SELECT DISTINCT dt.document.documentId FROM DocumentTag dt WHERE dt.tag.tagId IN :tagIds")
	List<Long> findDocumentIdsByTagIdIn(@Param("tagIds") List<Long> tagIds);
}
