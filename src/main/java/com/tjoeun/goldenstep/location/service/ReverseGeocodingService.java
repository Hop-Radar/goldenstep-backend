package com.tjoeun.goldenstep.location.service;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
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
public class ReverseGeocodingService {

	private static final BigDecimal MAX_LAT = new BigDecimal("90");
	private static final BigDecimal MAX_LNG = new BigDecimal("180");

    private final RestClient restClient;
    private final JsonMapper jsonMapper;
    
	ReverseGeocodingService(RestClient restClient, JsonMapper jsonMapper) {
        this.restClient = restClient;
        this.jsonMapper = jsonMapper;
    }

    public ReverseGeocodingService(
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

        this.restClient = restClientBuilder
                .requestFactory(requestFactory)
                .baseUrl("https://maps.apigw.ntruss.com")
                .defaultHeader(
                        "X-NCP-APIGW-API-KEY-ID",
                        clientId
                )
                .defaultHeader(
                        "X-NCP-APIGW-API-KEY",
                        clientSecret
                )
                .build();
    }

    public String findAddress(
            BigDecimal lat,
            BigDecimal lng
    ) {
        validateCoordinates(lat, lng);

		String coordinates = lng.toPlainString() + "," + lat.toPlainString();

        try {
            String body = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/map-reversegeocode/v2/gc")
                            .queryParam(
                                    "coords",
                                    "{coordinates}"
                            )
                            .queryParam(
                                    "sourcecrs",
                                    "epsg:4326"
                            )
                            .queryParam(
                                    "orders",
                                    "roadaddr,addr"
                            )
                            .queryParam("output", "json")
                            .build(coordinates))
                    .retrieve()
                    .body(String.class);

            if (body == null || body.isBlank()) {
                log.warn("네이버 주소 조회 응답이 비어 있습니다.");
                return null;
            }

            JsonNode root = jsonMapper.readTree(body);

            if (root == null) {
                log.warn("네이버 주소 조회 응답에 내용이 없습니다.");
                return null;
            }

            int statusCode =
                    root.path("status").path("code").asInt(-1);

			if (statusCode == 3) {
				return null;
			}

            if (statusCode != 0) {
                log.warn(
                        "네이버 주소 조회 실패: 응답 코드 {}",
                        statusCode
                );
                return null;
            }

            JsonNode results = root.path("results");

            if (!results.isArray()) {
                log.warn("네이버 주소 조회 results 형식이 올바르지 않습니다.");
                return null;
            }

			String roadAddress = findAddressByType(results, "roadaddr");

            if (roadAddress != null) {
                return roadAddress;
            }

            return findAddressByType(results, "addr");

        } catch (RestClientResponseException exception) {
            log.warn(
                    "네이버 주소 조회 실패: HTTP {}",
                    exception.getStatusCode()
            );
            return null;

        } catch (RestClientException | JacksonException exception) {
            log.warn(
                    "네이버 주소 조회 연결 또는 응답 처리에 실패했습니다.",
                    exception
            );
            return null;
        }
    }

    private String findAddressByType(
            JsonNode results,
            String type
    ) {
        for (JsonNode result : results) {
            if (!type.equals(text(result, "name"))) {
                continue;
            }

            String address = buildAddress(result, type);

            if (address != null) {
                return address;
            }
        }

        return null;
    }

    private String buildAddress(
            JsonNode result,
            String type
    ) {
        JsonNode region = result.path("region");
        JsonNode land = result.path("land");

        String area1 = text(region.path("area1"), "name");
        String area2 = text(region.path("area2"), "name");
        String area3 = text(region.path("area3"), "name");
        String area4 = text(region.path("area4"), "name");

        String number1 = text(land, "number1");
        String number2 = text(land, "number2");

        if (area1.isBlank() || number1.isBlank()) {
            return null;
        }

        String number = number2.isBlank()
                ? number1
                : number1 + "-" + number2;

        if ("roadaddr".equals(type)) {
            String roadName = text(land, "name");

            if (roadName.isBlank()) {
                return null;
            }

            // 도로명 주소에는 읍·면을 포함하고 동·리는 제외합니다.
            String township =
                    area3.endsWith("읍") || area3.endsWith("면")
                            ? area3
                            : "";

            return join(
                    area1,
                    area2,
                    township,
                    roadName,
                    number
            );
        }

        if (area3.isBlank()) {
            return null;
        }

        String lotNumber = "2".equals(text(land, "type"))
                ? "산" + number
                : number;

        return join(
                area1,
                area2,
                area3,
                area4,
                lotNumber
        );
    }

	private String text(JsonNode node, String field) {
		return node.path(field).asString("").trim();
	}

    private String join(String... parts) {
        return Arrays.stream(parts)
                .filter(part -> !part.isBlank())
                .collect(Collectors.joining(" "));
    }

	private void validateCoordinates(BigDecimal lat, BigDecimal lng) {
        if (lat == null || lng == null) {
			throw new RestException(ErrorCode.ADDRESS_COORDINATES_MISSING);
        }

		if (lat.abs().compareTo(MAX_LAT) > 0 || lng.abs().compareTo(MAX_LNG) > 0) {
			throw new RestException(ErrorCode.INVALID_ADDRESS_COORDINATES);
        }
    }
}