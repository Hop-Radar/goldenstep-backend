package com.tjoeun.goldenstep.search.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "search_session")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SearchSession {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(name = "recovery_token_hash", nullable = false, length = 64, columnDefinition = "char(64)")
	private String recoveryTokenHash;
	
	@Column(name = "last_lat", nullable = false, precision = 10, scale = 7)
	private BigDecimal lastLat;
	
	@Column(name = "last_lng", nullable = false, precision = 10, scale = 7)
	private BigDecimal lastLng;
	
	@Column(name = "last_address", length = 255)
	private String lastAddress;
	
	@Column(name = "last_seen_at", nullable = false)
	private LocalDateTime lastSeenAt;
	
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "person_attributes", nullable = false, columnDefinition = "json")
	private Map<String, Object> personAttributes;
	
	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;
	
	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;
}
