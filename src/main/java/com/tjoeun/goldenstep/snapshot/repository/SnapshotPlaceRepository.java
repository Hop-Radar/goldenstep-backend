package com.tjoeun.goldenstep.snapshot.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tjoeun.goldenstep.snapshot.entity.Snapshot;
import com.tjoeun.goldenstep.snapshot.entity.SnapshotPlace;

public interface SnapshotPlaceRepository extends JpaRepository<SnapshotPlace, Long> {

    List<SnapshotPlace> findBySnapshotOrderByPriorityRankAsc(Snapshot snapshot);
}
