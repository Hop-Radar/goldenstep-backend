package com.tjoeun.goldenstep.location.service;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.tjoeun.goldenstep.global.exception.ErrorCode;
import com.tjoeun.goldenstep.global.exception.RestException;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Service
public class PlaceNameResolver {

    private static final Pattern BUILDING_CODE = Pattern.compile(
            "^(?:\\d+\\s+)?B\\d{8,}[A-Z0-9]*$",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern POI_CODE = Pattern.compile(
            "^(?:BLDG|OSM)[-_]\\d+$",
            Pattern.CASE_INSENSITIVE
    );

    private static final Set<String> UNKNOWN_NAMES = Set.of(
            "unknown",
            "unnamed",
            "null",
            "none",
            "n/a",
            "알 수 없음",
            "알수없음",
            "이름 없음",
            "이름없음"
    );

    private final RestClient restClient;
    private final JsonMapper jsonMapper;

    // 테스트용 생성자
    PlaceNameResolver(RestClient restClient, JsonMapper jsonMapper) {
        this.restClient = restClient;
        this.jsonMapper = jsonMapper;
    }

    @Autowired
    public PlaceNameResolver(
            RestClient.Builder restClientBuilder,
            JsonMapper jsonMapper,
            @Value("${naver.maps.client-id}") String clientId,
            @Value("${naver.maps.client-secret}") String clientSecret
    ) {
        this.jsonMapper = jsonMapper;

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();

		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);

        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        this.restClient = restClientBuilder.clone()
                .requestFactory(requestFactory)
                .baseUrl("https://maps.apigw.ntruss.com")
                .defaultHeader("X-NCP-APIGW-API-KEY-ID", clientId)
                .defaultHeader("X-NCP-APIGW-API-KEY", clientSecret)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public String resolve(
            String originalName,
            String address,
            BigDecimal lat,
            BigDecimal lng
    ) {
        if (!isUnknownName(originalName)) {
            return originalName.strip();
        }

        validateCoordinates(lat, lng);

		String normalizedAddress = address == null ? "" : address.strip();

        if (normalizedAddress.isBlank()) {
            return coordinateFallback(lat, lng);
        }

        try {
            String buildingName = findBuildingName(
                    normalizedAddress,
                    lat,
                    lng
            );

            return buildingName == null
                    ? normalizedAddress
                    : buildingName;

        } catch (RestException exception) {
            if (!isRecoverableGeocodingError(exception.getErrorCode())) {
                throw exception;
            }

            Throwable cause = exception.getCause();

            if (cause instanceof RestClientResponseException responseException) {
                log.warn(
                        "건물명 조회 실패로 주소를 사용합니다. code={}, httpStatus={}",
                        exception.getErrorCode().getDivisionCode(),
                        responseException.getStatusCode().value()
                );
            } else {
                log.warn(
                        "건물명 조회 실패로 주소를 사용합니다. code={}, causeType={}",
                        exception.getErrorCode().getDivisionCode(),
						cause == null ? "없음" : cause.getClass().getSimpleName()
                );
            }

            log.debug("건물명 조회 실패 상세", exception);

            return normalizedAddress;
        }
    }

    public boolean isUnknownName(String name) {
        if (name == null || name.isBlank()) {
            return true;
        }

        String normalized = name.strip().replaceAll("\\s+", " ");

        return UNKNOWN_NAMES.contains(
                        normalized.toLowerCase(Locale.ROOT)
                )
                || BUILDING_CODE.matcher(normalized).matches()
                || POI_CODE.matcher(normalized).matches();
    }

    private String findBuildingName(
            String address,
            BigDecimal lat,
            BigDecimal lng
    ) {
        String body;

        try {
            String coordinate =
                    lng.toPlainString() + "," + lat.toPlainString();

            body = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/map-geocode/v2/geocode")
                            .queryParam("query", "{address}")
                            .queryParam("coordinate", "{coordinate}")
                            .queryParam("count", 1)
                            .build(address, coordinate))
                    .retrieve()
                    .body(String.class);

        } catch (RestClientResponseException exception) {
			throw new RestException(ErrorCode.GEOCODING_REQUEST_FAILED, exception);

        } catch (RestClientException exception) {
			throw new RestException(ErrorCode.GEOCODING_CONNECTION_FAILED, exception);
        }

		if (body == null || body.isBlank()) {
			throw new RestException(ErrorCode.INVALID_GEOCODING_RESPONSE);
        }

        JsonNode root;

        try {
            root = jsonMapper.readTree(body);
        } catch (JacksonException exception) {
			throw new RestException(ErrorCode.INVALID_GEOCODING_RESPONSE, exception);
        }

        if (root == null || !root.isObject()) {
			throw new RestException(ErrorCode.INVALID_GEOCODING_RESPONSE);
		}

        String status = root.path("status").asString("");

        if (status.isBlank()) {
			throw new RestException(ErrorCode.INVALID_GEOCODING_RESPONSE);
        }

        if (!"OK".equals(status)) {
			throw new RestException(ErrorCode.GEOCODING_REQUEST_FAILED);
        }

        JsonNode addresses = root.path("addresses");

        if (!addresses.isArray()) {
			throw new RestException(ErrorCode.INVALID_GEOCODING_RESPONSE);
        }

        if (addresses.size() == 0) {
            return null;
        }

        JsonNode selectedAddress = addresses.get(0);

        if (!selectedAddress.isObject()) {
			throw new RestException(ErrorCode.INVALID_GEOCODING_RESPONSE);
        }

        JsonNode elements = selectedAddress.path("addressElements");

        if (elements.isMissingNode() || elements.isNull()) {
            return null;
        }

        if (!elements.isArray()) {
			throw new RestException(ErrorCode.INVALID_GEOCODING_RESPONSE);
        }

        for (JsonNode element : elements) {
            if (!hasBuildingNameType(element)) {
                continue;
            }

			String buildingName = element.path("longName").asString("").strip();

            if (!isUnknownName(buildingName) && buildingName.length() <= 255) {
                return buildingName;
            }
        }

        return null;
    }

    private boolean hasBuildingNameType(JsonNode element) {
        return containsBuildingName(element.path("types"))
                || containsBuildingName(element.path("type"));
    }

    private boolean containsBuildingName(JsonNode types) {
        if (!types.isArray()) {
            return false;
        }

        for (JsonNode type : types) {
            if ("BUILDING_NAME".equals(type.asString(""))) {
                return true;
            }
        }

        return false;
    }

    private boolean isRecoverableGeocodingError(ErrorCode errorCode) {
        return errorCode == ErrorCode.GEOCODING_REQUEST_FAILED
                || errorCode == ErrorCode.GEOCODING_CONNECTION_FAILED
                || errorCode == ErrorCode.INVALID_GEOCODING_RESPONSE;
    }

    private void validateCoordinates(
            BigDecimal lat,
            BigDecimal lng
    ) {
        if (lat == null
                || lng == null
                || lat.abs().compareTo(BigDecimal.valueOf(90)) > 0
                || lng.abs().compareTo(BigDecimal.valueOf(180)) > 0) {

			throw new RestException(ErrorCode.INVALID_ANALYSIS_RESPONSE);
        }
    }

    private String coordinateFallback(
            BigDecimal lat,
            BigDecimal lng
    ) {
		return "위도 " + lat.toPlainString() + ", 경도 " + lng.toPlainString();
    }
}