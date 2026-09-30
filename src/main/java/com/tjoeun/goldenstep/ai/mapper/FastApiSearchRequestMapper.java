package com.tjoeun.goldenstep.ai.mapper;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.tjoeun.goldenstep.ai.dto.request.AnalysisPredictionRequest;
import com.tjoeun.goldenstep.ai.dto.request.FastApiSearchRequest;
import com.tjoeun.goldenstep.global.exception.ErrorCode;
import com.tjoeun.goldenstep.global.exception.RestException;

@Component
public class FastApiSearchRequestMapper {

	private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    private static final String OLDER_ADULT = "OLDER_ADULT";
    private static final String DISEASE_YES = "YES";
    private static final String DEMENTIA = "DEMENTIA";

	public FastApiSearchRequest map(AnalysisPredictionRequest request) {
        validateRequest(request);

		Map<String, Object> attributes = request.getPersonAttributes();

        String personType = attributes.get("personType").toString();

		if (OLDER_ADULT.equals(personType) && DISEASE_YES.equals(attributes.get("diseaseStatus").toString())) {
			personType = DEMENTIA;
		}

        Duration elapsed = Duration.between(
                request.getLastSeenAt(),
                request.getTargetAt()
        );

        if (elapsed.isNegative() || elapsed.isZero()) {
			throw new RestException(ErrorCode.INVALID_ANALYSIS_ELAPSED_TIME);
        }

        double elapsedHours =
                (elapsed.getSeconds()
                        + elapsed.getNano() / 1_000_000_000.0)
                        / 3600.0;

        String lastSeenTime = request.getLastSeenAt()
                .atZone(SERVICE_ZONE)
                .toOffsetDateTime()
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        FastApiSearchRequest.LastSeenLocation location =
                new FastApiSearchRequest.LastSeenLocation(
                        request.getLastLat(),
                        request.getLastLng()
                );

        FastApiSearchRequest.MissingPerson missingPerson =
                new FastApiSearchRequest.MissingPerson(
                        personType,
                        location,
                        lastSeenTime,
                        elapsedHours
                );

        return new FastApiSearchRequest(
                UUID.randomUUID().toString(),
                missingPerson
        );
    }

    private void validateRequest(
            AnalysisPredictionRequest request
    ) {
        if (request == null) {
			throw new RestException(ErrorCode.ANALYSIS_REQUEST_MISSING);
        }

        if (request.getTimePoint() == null
                || request.getTargetAt() == null
                || request.getLastSeenAt() == null
                || request.getLastLat() == null
                || request.getLastLng() == null
                || request.getPersonAttributes() == null) {

			throw new RestException(ErrorCode.INVALID_ANALYSIS_REQUEST);
        }

		Map<String, Object> attributes = request.getPersonAttributes();

        Object personType = attributes.get("personType");
        Object diseaseStatus = attributes.get("diseaseStatus");

        if (personType == null || personType.toString().isBlank()
                || diseaseStatus == null
                || diseaseStatus.toString().isBlank()) {

			throw new RestException(ErrorCode.INVALID_ANALYSIS_REQUEST);
        }

        if (!isWithinRange(request.getLastLat(), 90)
                || !isWithinRange(request.getLastLng(), 180)) {

			throw new RestException(ErrorCode.INVALID_ANALYSIS_REQUEST);
        }
    }

	private boolean isWithinRange(BigDecimal coordinate, int maximum) {
        return coordinate.abs()
                .compareTo(BigDecimal.valueOf(maximum)) <= 0;
    }
}