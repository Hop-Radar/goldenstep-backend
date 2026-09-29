package com.tjoeun.goldenstep.analysis.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tjoeun.goldenstep.analysis.entity.AnalysisRun;

public interface AnalysisRunRepository extends JpaRepository<AnalysisRun, Long> {

}
