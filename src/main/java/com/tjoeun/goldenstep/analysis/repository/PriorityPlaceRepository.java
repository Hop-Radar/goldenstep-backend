package com.tjoeun.goldenstep.analysis.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tjoeun.goldenstep.analysis.entity.PriorityPlace;
import com.tjoeun.goldenstep.analysis.entity.TimeResult;
import com.tjoeun.goldenstep.search.entity.SearchSession;

import jakarta.persistence.LockModeType;

public interface PriorityPlaceRepository extends JpaRepository<PriorityPlace, Long> {

	List<PriorityPlace> findByTimeResultOrderByPriorityRankAsc(TimeResult timeResult);
	
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select place
            from PriorityPlace place
            where place.id = :placeId
              and place.timeResult.analysisRun.searchSession = :searchSession
            """)
    Optional<PriorityPlace> findOwnedPlaceForUpdate(
            @Param("placeId") Long placeId,
            @Param("searchSession") SearchSession searchSession
    );
}
