package com.tjoeun.goldenstep.location.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.HtmlUtils;

import com.tjoeun.goldenstep.global.exception.ErrorCode;
import com.tjoeun.goldenstep.global.exception.RestException;
import com.tjoeun.goldenstep.location.dto.response.PlaceSearchResponse;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Service
public class PlaceSearchService {

    private static final BigDecimal MAX_LAT = new BigDecimal("90");
    private static final BigDecimal MAX_LNG = new BigDecimal("180");

    private final RestClient restClient;
    private final JsonMapper jsonMapper;

    public PlaceSearchService(
            RestClient.Builder restClientBuilder,
            JsonMapper jsonMapper,
            @Value("${naver.api-hub.client-id}") String clientId,
            @Value("${naver.api-hub.client-secret}") String clientSecret
    ) {
        this.jsonMapper = jsonMapper;

        this.restClient = restClientBuilder
                .baseUrl("https://naverapihub.apigw.ntruss.com")
                .defaultHeader("X-NCP-APIGW-API-KEY-ID", clientId)
                .defaultHeader("X-NCP-APIGW-API-KEY", clientSecret)
                .build();
    }
    public List<PlaceSearchResponse> search(String query) {
        if (query == null) {
            throw new IllegalArgumentException("장소 검색어를 입력해주세요.");
        }

        String keyword = query.trim();

        if (keyword.length() < 2 || keyword.length() > 100) {
			throw new IllegalArgumentException("장소 검색어는 2자 이상 100자 이하로 입력해주세요.");
        }

        try {
            ResponseEntity<String> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search/v1/local")
                            .queryParam("query", keyword)
                            .queryParam("display", 5)
                            .queryParam("start", 1)
                            .queryParam("sort", "random")
                            .queryParam("format", "json")
                            .build())
                    .retrieve()
                    .toEntity(String.class);

            String body = response.getBody();

            if (body == null || body.isBlank()) {
                throw new RestException(ErrorCode.PLACE_SEARCH_FAILED);
            }

            Map<?, ?> result = jsonMapper.readValue(body, Map.class);

            if (!(result.get("items") instanceof List<?> items)) {
                throw new RestException(ErrorCode.PLACE_SEARCH_FAILED);
            }

            return items.stream()
                    .filter(Map.class::isInstance)
                    .map(item -> toPlace((Map<?, ?>) item))
                    .toList();

        } catch (RestClientResponseException exception) {
            log.error(
                    "NAVER API HUB 지역 검색 실패: HTTP {}, 응답 {}",
                    exception.getStatusCode(),
                    exception.getResponseBodyAsString()
            );
            throw new RestException(ErrorCode.PLACE_SEARCH_FAILED);

        } catch (RestClientException | JacksonException | NumberFormatException exception) {
            log.error("NAVER API HUB 장소 검색 응답 처리 실패", exception);
            throw new RestException(ErrorCode.PLACE_SEARCH_FAILED);
        }
    }

    private PlaceSearchResponse toPlace(Map<?, ?> item) {
        String name = HtmlUtils.htmlUnescape(
                text(item.get("title")).replaceAll("<[^>]*>", "")
        );

        BigDecimal lat = parseCoordinate(item.get("mapy"), MAX_LAT);
        BigDecimal lng = parseCoordinate(item.get("mapx"), MAX_LNG);

        return new PlaceSearchResponse(
                name,
                text(item.get("roadAddress")),
                text(item.get("address")),
                lat,
                lng
        );
    }

    private BigDecimal parseCoordinate(
            Object rawValue,
            BigDecimal maximum
    ) {
		BigDecimal coordinate = new BigDecimal(text(rawValue)).movePointLeft(7);

        if (coordinate.abs().compareTo(maximum) > 0) {
            throw new RestException(ErrorCode.PLACE_SEARCH_FAILED);
        }

        return coordinate;
    }

    private String text(Object value) {
        return value == null ? "" : value.toString();
    }
}