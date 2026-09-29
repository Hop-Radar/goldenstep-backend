package com.tjoeun.goldenstep.analysis.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tjoeun.goldenstep.analysis.entity.AnalysisRun;
import com.tjoeun.goldenstep.analysis.entity.TimePoint;
import com.tjoeun.goldenstep.analysis.entity.TimeResult;

public interface TimeResultRepository extends JpaRepository<TimeResult, Long> {

	boolean existsByAnalysisRunAndTimePoint(AnalysisRun analysisRun, TimePoint timePoint);
}
