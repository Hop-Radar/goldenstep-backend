package com.tjoeun.goldenstep.analysis.dto.response;

import java.math.BigDecimal;

import com.tjoeun.goldenstep.analysis.entity.PriorityPlace;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "추천 장소")
public class PriorityPlaceResponse {

    private final Long placeId;
    private final String poiId;
    private final Byte priorityRank;
    private final String name;
    private final String address;
    private final BigDecimal lat;
    private final BigDecimal lng;
    private final BigDecimal score;

    public PriorityPlaceResponse(PriorityPlace place) {
        this.placeId = place.getId();
        this.poiId = place.getPoiId();
        this.priorityRank = place.getPriorityRank();
        this.name = place.getName();
        this.address = place.getAddress();
        this.lat = place.getLat();
        this.lng = place.getLng();
        this.score = place.getScore();
    }
}