package com.se1908.group01.repository;

import com.se1908.group01.entity.ChatSession;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Truy vấn session theo user và loại bỏ session đã soft-delete. */
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

	Optional<ChatSession> findBySessionIdAndUserIdAndIsDeletedFalse(Long sessionId, Long userId);

	List<ChatSession> findByUserIdAndIsDeletedFalseOrderByUpdatedAtDesc(Long userId);
}
