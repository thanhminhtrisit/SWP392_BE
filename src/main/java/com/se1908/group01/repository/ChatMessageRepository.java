package com.se1908.group01.repository;

import com.se1908.group01.entity.ChatMessage;
import com.se1908.group01.enums.ChatMessageStatus;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository lưu lịch sử message và lấy cửa sổ message gần nhất cho conversation memory. */
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

	Page<ChatMessage> findByChatSessionSessionIdOrderByCreatedAtAscMessageIdAsc(
			Long sessionId,
			Pageable pageable
	);

	List<ChatMessage> findTop5ByChatSessionSessionIdAndStatusOrderByCreatedAtDescMessageIdDesc(
			Long sessionId,
			ChatMessageStatus status
	);
}
