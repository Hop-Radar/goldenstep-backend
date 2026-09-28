package com.tjoeun.goldenstep.search.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tjoeun.goldenstep.search.entity.SearchSession;

public interface SearchSessionRepository extends JpaRepository<SearchSession, Long> {

}
