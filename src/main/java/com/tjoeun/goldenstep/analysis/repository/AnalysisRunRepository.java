package com.tjoeun.goldenstep.analysis.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tjoeun.goldenstep.analysis.entity.AnalysisRun;
import com.tjoeun.goldenstep.search.entity.SearchSession;


public interface AnalysisRunRepository extends JpaRepository<AnalysisRun, Long> {

	Optional<AnalysisRun> findByIdAndSearchSession(Long id, SearchSession searchSession);
}
