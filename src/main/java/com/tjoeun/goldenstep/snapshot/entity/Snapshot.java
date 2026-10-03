package com.tjoeun.goldenstep.snapshot.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.tjoeun.goldenstep.search.entity.SearchSession;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;
}