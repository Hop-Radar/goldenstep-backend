package com.tjoeun.goldenstep.global.service;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.tjoeun.goldenstep.global.dto.response.HealthResponse;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Service
public class HealthService {

    private final RestClient fastApiClient;
    private final JsonMapper jsonMapper;

    @Autowired
    public HealthService(
            RestClient.Builder builder,
            JsonMapper jsonMapper,
            @Value("${goldenstep.ai.base-url:http://localhost:8000}")
            String baseUrl,
            @Value("${goldenstep.health.connect-timeout-ms:1000}")
            long connectTimeoutMs,
            @Value("${goldenstep.health.read-timeout-ms:3000}")
            long readTimeoutMs
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();

        JdkClientHttpRequestFactory factory =
                new JdkClientHttpRequestFactory(httpClient);

        factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        this.fastApiClient = builder.clone()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();

        this.jsonMapper = jsonMapper;
    }

    HealthService(
            RestClient fastApiClient,
            JsonMapper jsonMapper
    ) {
        this.fastApiClient = fastApiClient;
        this.jsonMapper = jsonMapper;
    }

    public HealthResponse check() {
        try {
            ResponseEntity<String> response = fastApiClient.get()
                    .uri("/health")
                    .retrieve()
                    .toEntity(String.class);

            String body = response.getBody();

            if (response.getStatusCode().value() != 200
                    || body == null
                    || body.isBlank()) {
                return unavailable();
            }

            JsonNode root = jsonMapper.readTree(body);

            if (root != null
                    && "UP".equals(root.path("status").asText())) {
                return new HealthResponse(
                        "UP",
                        "UP",
                        "UP",
                        Instant.now()
                );
            }

            return unavailable();

        } catch (RestClientException | JacksonException exception) {
            log.debug(
                    "FastAPI 상태 확인 실패: {}",
                    exception.getMessage()
            );

            return unavailable();
        }
    }

    private HealthResponse unavailable() {
        return new HealthResponse(
                "DEGRADED",
                "UP",
                "UNAVAILABLE",
                Instant.now()
        );
    }
}