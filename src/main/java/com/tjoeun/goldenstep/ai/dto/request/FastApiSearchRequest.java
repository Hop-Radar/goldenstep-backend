package com.tjoeun.goldenstep.ai.dto.request;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

@Getter
public class FastApiSearchRequest {

    @JsonProperty("request_id")
    private final String requestId;

    @JsonProperty("missing_person")
    private final MissingPerson missingPerson;

	public FastApiSearchRequest(String requestId, MissingPerson missingPerson) {
        this.requestId = requestId;
        this.missingPerson = missingPerson;
    }

    @Getter
    public static class MissingPerson {

        @JsonProperty("person_type")
        private final String personType;

        @JsonProperty("last_seen_location")
        private final LastSeenLocation lastSeenLocation;

        @JsonProperty("last_seen_time")
        private final String lastSeenTime;

        @JsonProperty("elapsed_hours")
        private final double elapsedHours;

        public MissingPerson(
                String personType,
                LastSeenLocation lastSeenLocation,
                String lastSeenTime,
                double elapsedHours
        ) {
            this.personType = personType;
            this.lastSeenLocation = lastSeenLocation;
            this.lastSeenTime = lastSeenTime;
            this.elapsedHours = elapsedHours;
        }
    }

    @Getter
    public static class LastSeenLocation {

        private final BigDecimal lat;
        private final BigDecimal lon;

		public LastSeenLocation(BigDecimal lat, BigDecimal lon) {
			this.lat = lat;
			this.lon = lon;
		}
    }
}