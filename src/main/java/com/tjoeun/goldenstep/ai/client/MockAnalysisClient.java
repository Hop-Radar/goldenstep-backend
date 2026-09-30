package com.tjoeun.goldenstep.ai.client;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.tjoeun.goldenstep.ai.dto.request.AnalysisPredictionRequest;
import com.tjoeun.goldenstep.ai.dto.response.AnalysisPredictionResponse;
import com.tjoeun.goldenstep.ai.dto.response.AnalysisPredictionResponse.PriorityPoint;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
@Profile("mock")
public class MockAnalysisClient implements AnalysisClient {

	private static final BigDecimal MAX_LAT = new BigDecimal("90");

	private static final BigDecimal MAX_LNG = new BigDecimal("180");

    private final JsonMapper jsonMapper;
    private final Resource responseResource;

    public MockAnalysisClient(
            JsonMapper jsonMapper,
            @Value(
                    "${goldenstep.analysis.mock-response:"
                    + "classpath:mock/mock_search_response.json}"
            )
            Resource responseResource
    ) {
        this.jsonMapper = jsonMapper;
        this.responseResource = responseResource;
    }

    @Override
    public AnalysisPredictionResponse analyze(
            AnalysisPredictionRequest request
    ) {
        if (request == null) {
			throw new IllegalArgumentException("분석 요청 정보가 필요합니다.");
        }

        AnalysisPredictionResponse prediction;

        try (InputStream inputStream =
                responseResource.getInputStream()) {

            prediction = jsonMapper.readValue(
                    inputStream,
                    AnalysisPredictionResponse.class
            );

        } catch (IOException | JacksonException exception) {
			throw new IllegalStateException("모의 분석 응답 파일을 읽지 못했습니다. 파일의 위치와 JSON 형식을 확인해주세요.", exception);
        }

        validateResponse(prediction);

        return prediction;
    }

	private void validateResponse(AnalysisPredictionResponse prediction) {
        if (prediction == null) {
			throw invalidResponse("분석 결과가 없습니다.");
        }

        if (!"SUCCESS".equals(prediction.getStatus())) {
			throw invalidResponse("status는 SUCCESS여야 합니다.");
        }

        if (prediction.getSummary() == null) {
			throw invalidResponse("summary가 없습니다.");
        }

        if (prediction.getPriorityPoints() == null) {
			throw invalidResponse("priority_points가 없습니다. 추천 장소가 없다면 빈 배열로 작성해주세요.");
        }

        if (prediction.getPriorityPoints().size() > 3) {
			throw invalidResponse("추천 장소는 최대 3개여야 합니다.");
        }

        Set<Byte> ranks = new HashSet<>();
        Set<String> poiIds = new HashSet<>();

        for (PriorityPoint point :
                prediction.getPriorityPoints()) {

            validatePoint(point);

            if (!ranks.add(point.getRank())) {
				throw invalidResponse("추천 장소 순위가 중복되었습니다.");
            }

            if (!poiIds.add(point.getPoiId())) {
				throw invalidResponse("추천 장소 식별자가 중복되었습니다.");
            }
        }
    }

    private void validatePoint(PriorityPoint point) {
        if (point == null
                || point.getRank() == null
                || point.getPoiId() == null
                || point.getPoiId().isBlank()
                || point.getName() == null
                || point.getName().isBlank()
                || point.getLocation() == null
                || point.getLocation().getLat() == null
                || point.getLocation().getLon() == null) {

            throw invalidResponse(
                    "추천 장소의 순위, 식별자, 이름, 위도, 경도는 필수입니다."
            );
        }

        if (point.getRank() < 1 || point.getRank() > 3) {
            throw invalidResponse(
                    "추천 장소 순위는 1~3이어야 합니다."
            );
        }

        if (point.getLocation().getLat().abs()
                .compareTo(MAX_LAT) > 0
                || point.getLocation().getLon().abs()
                .compareTo(MAX_LNG) > 0) {

			throw invalidResponse("추천 장소의 위도 또는 경도가 유효 범위를 벗어났습니다.");
        }
    }

    private IllegalStateException invalidResponse(
            String reason
    ) {
		return new IllegalStateException("모의 분석 응답이 올바르지 않습니다. " + reason);
    }
}