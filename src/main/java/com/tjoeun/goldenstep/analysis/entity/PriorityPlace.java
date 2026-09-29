package com.tjoeun.goldenstep.analysis.entity;

import java.math.BigDecimal;

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
@Table(name = "priority_place")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PriorityPlace {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "result_id", nullable = false)
	private TimeResult timeResult;
	
	@Column(name = "poi_id", nullable = false, length = 100)
	private String poiId;
	
	@Column(name = "priority_rank", nullable = false)
	private Byte priorityRank;
	
	@Column(name = "name", nullable = false, length = 150)
	private String name;
	
	@Column(name = "address", length = 255)
	private String address;
	
	@Column(name = "lat", nullable = false, precision = 10, scale = 7)
    private BigDecimal lat;

    @Column(name = "lng", nullable = false, precision = 10, scale = 7)
    private BigDecimal lng;

    @Column(name = "score", precision = 10, scale = 7)
    private BigDecimal score;
    
    public PriorityPlace(
            TimeResult timeResult,
            String poiId,
            Byte priorityRank,
            String name,
            String address,
            BigDecimal lat,
            BigDecimal lng,
            BigDecimal score
    ) {
        this.timeResult = timeResult;
        this.poiId = poiId;
        this.priorityRank = priorityRank;
        this.name = name;
        this.address = address;
        this.lat = lat;
        this.lng = lng;
        this.score = score;
    }
}
