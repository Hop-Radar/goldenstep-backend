package com.tjoeun.goldenstep.ai.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnalysisPredictionResponse {

    private String status;

    private Summary summary;

    @JsonProperty("boundary_zone")
    private Map<String, Object> boundaryZone;

    @JsonProperty("priority_points")
    private List<PriorityPoint> priorityPoints;

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Summary {

        @JsonProperty("reliability_status")
        private String reliabilityStatus;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PriorityPoint {

        private Byte rank;

        @JsonProperty("poi_id")
        private String poiId;

        private String name;

        private Location location;

        private BigDecimal score;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Location {

        private BigDecimal lat;

        private BigDecimal lon;
    }
}