package com.se1908.group01.repository;

import com.se1908.group01.entity.AiTokenUsage;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiTokenUsageRepository extends JpaRepository<AiTokenUsage, Long> {

	@Query("""
			SELECT COALESCE(SUM(u.estimatedTokens), 0)
			FROM AiTokenUsage u
			WHERE u.userId = :userId
			  AND u.createdAt >= :from
			""")
	long sumEstimatedTokensSince(
			@Param("userId") Long userId,
			@Param("from") Instant from
	);
}
