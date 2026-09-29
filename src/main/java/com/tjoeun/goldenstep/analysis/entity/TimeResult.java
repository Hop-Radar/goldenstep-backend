package com.tjoeun.goldenstep.analysis.entity;

import java.time.LocalDateTime;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
@Table(name = "time_result")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TimeResult {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "run_id", nullable = false)
	private AnalysisRun analysisRun;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "time_point", nullable = false)
	private TimePoint timePoint;
	
	@Column(name = "target_at", nullable = false)
	private LocalDateTime targetAt;
	
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "boundary_zone", columnDefinition = "json")
	private Map<String, Object> boundaryZone;
	
	@Column(name = "reliability_status", length = 30)
	private String reliabilityStatus;
	
	public TimeResult(
	        AnalysisRun analysisRun,
	        TimePoint timePoint,
	        LocalDateTime targetAt,
	        Map<String, Object> boundaryZone,
	        String reliabilityStatus
	) {
	    this.analysisRun = analysisRun;
	    this.timePoint = timePoint;
	    this.targetAt = targetAt;
	    this.boundaryZone = boundaryZone;
	    this.reliabilityStatus = reliabilityStatus;
	}
}
