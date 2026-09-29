package com.tjoeun.goldenstep.search.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tjoeun.goldenstep.search.entity.SearchSession;

public interface SearchSessionRepository extends JpaRepository<SearchSession, Long> {

	Optional<SearchSession> findByRecoveryTokenHash(String recoveryTokenHash);
}
