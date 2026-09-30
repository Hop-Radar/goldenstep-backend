package com.tjoeun.goldenstep.analysis.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tjoeun.goldenstep.analysis.entity.PlaceCheck;
import com.tjoeun.goldenstep.analysis.entity.PriorityPlace;

public interface PlaceCheckRepository extends JpaRepository<PlaceCheck, Long> {

	Optional<PlaceCheck> findByPriorityPlace(PriorityPlace priorityPlace);
}
