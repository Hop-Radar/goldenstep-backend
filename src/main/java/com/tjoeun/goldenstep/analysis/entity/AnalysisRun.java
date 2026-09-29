package com.tjoeun.goldenstep.analysis.entity;

import java.time.LocalDateTime;

import com.tjoeun.goldenstep.search.entity.SearchSession;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "analysis_run")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnalysisRun {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "session_id", nullable = false)
	private SearchSession searchSession;
	
	@Column(name = "requested_at", nullable = false)
	private LocalDateTime requestedAt;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private Status status;
	
	@Column(name = "completed_at")
	private LocalDateTime completedAt;
	
	public AnalysisRun(SearchSession searchSession, LocalDateTime requestedAt) {
        this.searchSession = searchSession;
        this.requestedAt = requestedAt;
        this.status = Status.PROCESSING;
    }
	
	public void complete(LocalDateTime completedAt) {
		if (this.status != Status.PROCESSING) {
			throw new IllegalStateException("진행 중인 분석만 완료할 수 있습니다.");
		}

	    this.status = Status.COMPLETED;
	    this.completedAt = completedAt;
	}
}
