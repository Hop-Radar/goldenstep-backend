package com.tjoeun.goldenstep.snapshot.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tjoeun.goldenstep.search.entity.SearchSession;
import com.tjoeun.goldenstep.snapshot.entity.Snapshot;

public interface SnapshotRepository extends JpaRepository<Snapshot, Long> {

    Optional<Snapshot> findByTokenHash(String tokenHash);
    
    Optional<Snapshot> findByIdAndSearchSession(Long snapshotId, SearchSession searchSession);
}
