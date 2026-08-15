package com.se1908.group01.repository;

import com.se1908.group01.entity.ChatMessageSource;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository lưu và đọc citation theo các message đã tải về UI. */
public interface ChatMessageSourceRepository extends JpaRepository<ChatMessageSource, Long> {

	List<ChatMessageSource> findByMessageMessageIdIn(List<Long> messageIds);
}
