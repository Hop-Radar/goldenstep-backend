package com.tjoeun.goldenstep.analysis.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tjoeun.goldenstep.analysis.entity.PriorityPlace;
import com.tjoeun.goldenstep.analysis.entity.TimeResult;

public interface PriorityPlaceRepository extends JpaRepository<PriorityPlace, Long> {

	List<PriorityPlace> findByTimeResultOrderByPriorityRankAsc(TimeResult timeResult);
}
