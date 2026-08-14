package com.se1908.group01.repository;

import com.se1908.group01.entity.Tag;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, Long> {

	List<Tag> findByUserIdOrderByNameAsc(Long userId);

	Optional<Tag> findByTagIdAndUserId(Long tagId, Long userId);

	boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);

	boolean existsByUserIdAndNameIgnoreCaseAndTagIdNot(Long userId, String name, Long tagId);
}
