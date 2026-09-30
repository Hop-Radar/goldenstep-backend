package com.tjoeun.goldenstep.search.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tjoeun.goldenstep.search.entity.SearchSession;

public interface SearchSessionRepository extends JpaRepository<SearchSession, Long> {

	Optional<SearchSession> findByRecoveryTokenHash(String recoveryTokenHash);
	
	@Modifying
	@Query(
	        value = """
	                DELETE FROM search_session
	                WHERE expires_at <= :expiredAt
	                ORDER BY expires_at, id
	                LIMIT :batchSize
	                """,
	        nativeQuery = true
	)
	int deleteExpiredSessions(
	        @Param("expiredAt") LocalDateTime expiredAt,
	        @Param("batchSize") int batchSize
	);
}
