package com.tjoeun.goldenstep.snapshot.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.tjoeun.goldenstep.analysis.entity.TimePoint;
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
@Table(name = "snapshot")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Snapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private SearchSession searchSession;

    @Column(name = "token_hash", nullable = false, length = 64,
            columnDefinition = "char(64)")
    private String tokenHash;

    @Column(name = "last_lat", nullable = false, precision = 10, scale = 7)
    private BigDecimal lastLat;

    @Column(name = "last_lng", nullable = false, precision = 10, scale = 7)
    private BigDecimal lastLng;

    @Column(name = "last_address", length = 255)
    private String lastAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    @Column(name = "additional_info", length = 500)
    private String additionalInfo;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "time_point", length = 16)
    private TimePoint timePoint;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "boundary_zone", columnDefinition = "json")
    private Map<String, Object> boundaryZone;

    public Snapshot(SearchSession session, String tokenHash, TimePoint timePoint,
            Map<String, Object> boundaryZone, LocalDateTime createdAt) {
        this.searchSession = session;
        this.tokenHash = tokenHash;
        this.lastLat = session.getLastLat();
        this.lastLng = session.getLastLng();
        this.lastAddress = session.getLastAddress();
        this.lastSeenAt = session.getLastSeenAt();
        Object info = session.getPersonAttributes().get("physicalFeatures");
        this.additionalInfo = info instanceof String text ? text : null;
        this.timePoint = timePoint;
        this.boundaryZone = boundaryZone;
        this.createdAt = createdAt;
        this.expiresAt = createdAt.plusHours(12);
    }

    public boolean isUnavailable(LocalDateTime now) {
        return revokedAt != null || !expiresAt.isAfter(now);
    }

    public void revoke(LocalDateTime now) {
        if (revokedAt == null) {
            revokedAt = now;
        }
    }
}
