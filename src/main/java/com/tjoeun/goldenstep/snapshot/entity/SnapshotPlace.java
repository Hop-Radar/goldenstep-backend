package com.tjoeun.goldenstep.snapshot.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.tjoeun.goldenstep.analysis.dto.response.PriorityPlaceResponse;

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
@Table(name = "snapshot_place")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SnapshotPlace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "snapshot_id", nullable = false)
    private Snapshot snapshot;

    @Column(name = "priority_rank", nullable = false)
    private Byte priorityRank;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "lat", nullable = false, precision = 10, scale = 7)
    private BigDecimal lat;

    @Column(name = "lng", nullable = false, precision = 10, scale = 7)
    private BigDecimal lng;

    @Column(name = "checked_at")
    private LocalDateTime checkedAt;

    public SnapshotPlace(Snapshot snapshot, PriorityPlaceResponse place) {
        this.snapshot = snapshot;
        this.priorityRank = place.getPriorityRank();
        this.name = place.getName();
        this.address = place.getAddress();
        this.lat = place.getLat();
        this.lng = place.getLng();
        this.checkedAt = place.getCheckedAt();
    }
}
