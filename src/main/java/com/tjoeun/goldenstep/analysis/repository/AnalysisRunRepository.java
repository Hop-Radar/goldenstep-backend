package com.tjoeun.goldenstep.analysis.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tjoeun.goldenstep.analysis.entity.AnalysisRun;
import com.tjoeun.goldenstep.search.entity.SearchSession;

import jakarta.persistence.LockModeType;


public interface AnalysisRunRepository extends JpaRepository<AnalysisRun, Long> {

	Optional<AnalysisRun> findByIdAndSearchSession(Long id, SearchSession searchSession);
	
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			SELECT run
			FROM AnalysisRun run
			WHERE run.id = :runId
			""")
	Optional<AnalysisRun> findByIdForUpdate(@Param("runId") Long runId);
}
