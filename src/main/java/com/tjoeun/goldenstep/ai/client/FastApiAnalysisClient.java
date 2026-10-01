package com.tjoeun.goldenstep.ai.client;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.tjoeun.goldenstep.ai.dto.request.AnalysisPredictionRequest;
import com.tjoeun.goldenstep.ai.dto.request.FastApiSearchRequest;
import com.tjoeun.goldenstep.ai.dto.response.AnalysisPredictionResponse;
import com.tjoeun.goldenstep.ai.dto.response.AnalysisPredictionResponse.PriorityPoint;
import com.tjoeun.goldenstep.ai.mapper.FastApiSearchRequestMapper;
import com.tjoeun.goldenstep.global.exception.ErrorCode;
import com.tjoeun.goldenstep.global.exception.RestException;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
@Profile("fastapi & !mock")
public class FastApiAnalysisClient implements AnalysisClient {

	private static final String SEARCH_PATH = "/api/v1/simulation/search";
    private static final String HEALTH_PATH = "/health";
    private static final Integer CONNECT_TIMEOUT = 3;
    private static final Integer READ_TIMEOUT = 180;

	private static final BigDecimal MAX_LAT = BigDecimal.valueOf(90);
	private static final BigDecimal MAX_LNG = BigDecimal.valueOf(180);

    private final RestClient restClient;
    private final JsonMapper jsonMapper;
    private final FastApiSearchRequestMapper requestMapper;

    @Autowired
    public FastApiAnalysisClient(
            RestClient.Builder restClientBuilder,
            JsonMapper jsonMapper,
            FastApiSearchRequestMapper requestMapper,
            @Value("${goldenstep.ai.base-url:http://localhost:8000}")
            String baseUrl
    ) {
        this.jsonMapper = jsonMapper;
        this.requestMapper = requestMapper;

        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT))
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);

        requestFactory.setReadTimeout(Duration.ofSeconds(READ_TIMEOUT));

        this.restClient = restClientBuilder.clone()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();

        log.info(
				"[FastAPI 설정] baseUrl={}, connectTimeout={}초, " + "readTimeout={}초",
				baseUrl, CONNECT_TIMEOUT, READ_TIMEOUT
        );
    }

    FastApiAnalysisClient(
            RestClient restClient,
            JsonMapper jsonMapper,
            FastApiSearchRequestMapper requestMapper
    ) {
        this.restClient = restClient;
        this.jsonMapper = jsonMapper;
        this.requestMapper = requestMapper;
    }

    public void checkConnection() {
        long startedAt = System.nanoTime();

        log.info(
                "[FastAPI 연결 확인 시작] method=GET, path={}",
                HEALTH_PATH
        );

        try {
            ResponseEntity<String> response = restClient.get()
                    .uri(HEALTH_PATH)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toEntity(String.class);

            log.info(
                    "[FastAPI 연결 확인 성공] status={}, elapsedMs={}",
                    response.getStatusCode().value(),
                    elapsedMillis(startedAt)
            );

            log.debug(
                    "[FastAPI Health 응답]\n{}",
                    response.getBody()
            );

        } catch (RestClientResponseException exception) {
            log.warn(
                    "[FastAPI 연결됨 / Health HTTP 오류] "
                            + "status={}, elapsedMs={}, body={}",
                    exception.getStatusCode().value(),
                    elapsedMillis(startedAt),
                    exception.getResponseBodyAsString()
            );

        } catch (RestClientException exception) {
            log.warn(
                    "[FastAPI 연결 확인 실패] elapsedMs={}, cause={}",
                    elapsedMillis(startedAt),
                    rootCauseDescription(exception),
                    exception
            );
        }
    }

    @Override
    public AnalysisPredictionResponse analyze(
            AnalysisPredictionRequest request
    ) {
        FastApiSearchRequest apiRequest = requestMapper.map(request);
        String requestId = apiRequest.getRequestId();

        long startedAt = System.nanoTime();

        String stage = "REQUEST_SERIALIZATION";

        try {
            String requestBody =
                    jsonMapper.writeValueAsString(apiRequest);

            log.info(
                    "[FastAPI 요청 준비] requestId={}, timePoint={}, "
                            + "elapsedHours={}",
                    requestId,
                    request.getTimePoint(),
                    apiRequest.getMissingPerson().getElapsedHours()
            );

            log.debug(
                    "[FastAPI 요청 JSON] requestId={}\n{}",
                    requestId,
                    requestBody
            );

            stage = "HTTP_RESPONSE_RECEIVE";

            log.info(
                    "[FastAPI POST 시작] requestId={}, path={}",
                    requestId,
                    SEARCH_PATH
            );

            ResponseEntity<String> httpResponse = restClient.post()
                    .uri(SEARCH_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .toEntity(String.class);

            String responseBody = httpResponse.getBody();

            log.info(
                    "[FastAPI 응답 수신 완료] requestId={}, "
                            + "status={}, contentType={}, "
                            + "bodyChars={}, elapsedMs={}",
                    requestId,
                    httpResponse.getStatusCode().value(),
                    httpResponse.getHeaders().getContentType(),
                    responseBody == null ? 0 : responseBody.length(),
                    elapsedMillis(startedAt)
            );

            log.debug(
                    "[FastAPI 응답 JSON] requestId={}\n{}",
                    requestId,
                    responseBody
            );

            if (responseBody == null || responseBody.isBlank()) {
                throw invalidResponse(
                        requestId,
                        "응답 본문이 비어 있습니다."
                );
            }

            stage = "RESPONSE_DESERIALIZATION";
            long parsingStartedAt = System.nanoTime();

            log.info(
                    "[FastAPI DTO 변환 시작] requestId={}",
                    requestId
            );

            AnalysisPredictionResponse response =
                    jsonMapper.readValue(
                            responseBody,
                            AnalysisPredictionResponse.class
                    );

            log.info(
                    "[FastAPI DTO 변환 완료] requestId={}, elapsedMs={}",
                    requestId,
                    elapsedMillis(parsingStartedAt)
            );

            stage = "RESPONSE_VALIDATION";
            validateResponse(response, requestId);

            log.info(
                    "[FastAPI 분석 요청 완료] requestId={}, "
                            + "timePoint={}, reliability={}, "
                            + "placeCount={}, totalElapsedMs={}",
                    requestId,
                    request.getTimePoint(),
                    response.getSummary().getReliabilityStatus(),
                    response.getPriorityPoints().size(),
                    elapsedMillis(startedAt)
            );

            return response;

        } catch (RestClientResponseException exception) {
            log.error(
                    "[FastAPI HTTP 오류 응답 수신] requestId={}, "
                            + "stage={}, status={}, elapsedMs={}",
                    requestId,
                    stage,
                    exception.getStatusCode().value(),
                    elapsedMillis(startedAt),
                    exception
            );

            log.debug(
                    "[FastAPI 오류 응답 JSON] requestId={}\n{}",
                    requestId,
                    exception.getResponseBodyAsString()
            );

            throw new RestException(
                    ErrorCode.AI_ANALYSIS_REQUEST_FAILED
            );

        } catch (ResourceAccessException exception) {
            boolean timeout = isTimeout(exception);

            log.error(
                    "[FastAPI 통신 실패] requestId={}, stage={}, "
                            + "timeout={}, elapsedMs={}, cause={}",
                    requestId,
                    stage,
                    timeout,
                    elapsedMillis(startedAt),
                    rootCauseDescription(exception),
                    exception
            );

            throw new RestException(
                    timeout
                            ? ErrorCode.AI_ANALYSIS_TIMEOUT
                            : ErrorCode.AI_ANALYSIS_CONNECTION_FAILED
            );

        } catch (JacksonException exception) {
            log.error(
                    "[FastAPI JSON 처리 실패] requestId={}, "
                            + "stage={}, elapsedMs={}",
                    requestId,
                    stage,
                    elapsedMillis(startedAt),
                    exception
            );

            throw new RestException(
                    ErrorCode.INVALID_ANALYSIS_RESPONSE
            );

        } catch (RestClientException exception) {
            log.error(
                    "[FastAPI HTTP 처리 실패] requestId={}, "
                            + "stage={}, elapsedMs={}, cause={}",
                    requestId,
                    stage,
                    elapsedMillis(startedAt),
                    rootCauseDescription(exception),
                    exception
            );

            throw new RestException(
                    ErrorCode.AI_ANALYSIS_REQUEST_FAILED
            );
        }
    }

    private void validateResponse(
            AnalysisPredictionResponse response,
            String requestId
    ) {
        if (response == null) {
            throw invalidResponse(requestId, "분석 결과가 없습니다.");
        }

        if (!"SUCCESS".equals(response.getStatus())) {
            throw invalidResponse(
                    requestId,
                    "분석 상태가 SUCCESS가 아닙니다."
            );
        }

        if (response.getSummary() == null
                || response.getSummary().getReliabilityStatus() == null
                || response.getSummary()
                        .getReliabilityStatus().isBlank()) {
            throw invalidResponse(
                    requestId,
                    "분석 신뢰도 정보가 없습니다."
            );
        }

        if (response.getBoundaryZone() == null) {
            throw invalidResponse(requestId, "예측 영역이 없습니다.");
        }

        if (response.getPriorityPoints() == null
                || response.getPriorityPoints().size() > 3) {
            throw invalidResponse(
                    requestId,
                    "추천 장소 목록이 없거나 3개를 초과합니다."
            );
        }

        Set<Byte> ranks = new HashSet<>();
        Set<String> poiIds = new HashSet<>();

        for (PriorityPoint point : response.getPriorityPoints()) {
            if (point == null
                    || point.getRank() == null
                    || point.getPoiId() == null
                    || point.getPoiId().isBlank()
                    || point.getLocation() == null
                    || point.getLocation().getLat() == null
                    || point.getLocation().getLon() == null) {
                throw invalidResponse(
                        requestId,
                        "추천 장소의 필수 정보가 없습니다."
                );
            }

            int rank = point.getRank();

            if (rank < 1 || rank > 3
                    || !ranks.add(point.getRank())
                    || !poiIds.add(point.getPoiId())) {
                throw invalidResponse(
                        requestId,
                        "추천 장소 순위 또는 식별자가 올바르지 않습니다."
                );
            }

            if (point.getLocation().getLat().abs()
                        .compareTo(MAX_LAT) > 0
                    || point.getLocation().getLon().abs()
                        .compareTo(MAX_LNG) > 0) {
                throw invalidResponse(
                        requestId,
                        "추천 장소 좌표가 유효 범위를 벗어났습니다."
                );
            }

            if (point.getPoiId().length() > 100
                    || (point.getName() != null
                        && point.getName().length() > 150)) {

                throw invalidResponse(
                        requestId,
                        "추천 장소 식별자 또는 이름이 저장 가능한 길이를 초과합니다."
                );
            }
        }
    }

    private RestException invalidResponse(
            String requestId,
            String reason
    ) {
        log.error(
                "[FastAPI 응답 검증 실패] requestId={}, reason={}",
                requestId,
                reason
        );

        return new RestException(
                ErrorCode.INVALID_ANALYSIS_RESPONSE
        );
    }

    private boolean isTimeout(Throwable exception) {
        Throwable cause = exception;

        while (cause != null) {
            if (cause instanceof HttpTimeoutException
                    || cause instanceof SocketTimeoutException) {
                return true;
            }

            Throwable next = cause.getCause();

            if (next == cause) {
                break;
            }

            cause = next;
        }

        return false;
    }

    private static long elapsedMillis(long startedAt) {
        return TimeUnit.NANOSECONDS.toMillis(
                System.nanoTime() - startedAt
        );
    }

    private static String rootCauseDescription(
            Throwable exception
    ) {
        Throwable cause = exception;

        while (cause.getCause() != null
                && cause.getCause() != cause) {
            cause = cause.getCause();
        }

        return cause.getClass().getName()
                + ": " + cause.getMessage();
    }
}