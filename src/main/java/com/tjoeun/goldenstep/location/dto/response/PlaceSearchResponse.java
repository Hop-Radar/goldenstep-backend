package com.tjoeun.goldenstep.location.dto.response;

import java.math.BigDecimal;

import lombok.Getter;

@Getter
public class PlaceSearchResponse {

    private final String name;
    private final String roadAddress;
    private final String jibunAddress;
    private final BigDecimal lat;
    private final BigDecimal lng;

    public PlaceSearchResponse(
            String name,
            String roadAddress,
            String jibunAddress,
            BigDecimal lat,
            BigDecimal lng
    ) {
        this.name = name;
        this.roadAddress = roadAddress;
        this.jibunAddress = jibunAddress;
        this.lat = lat;
        this.lng = lng;
    }
}