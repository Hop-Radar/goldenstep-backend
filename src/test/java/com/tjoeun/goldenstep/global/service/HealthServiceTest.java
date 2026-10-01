package com.tjoeun.goldenstep.global.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.SocketTimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.tjoeun.goldenstep.global.dto.response.HealthResponse;

import tools.jackson.databind.json.JsonMapper;

class HealthServiceTest {

    private MockRestServiceServer server;
    private HealthService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("http://localhost:8000");

        server = MockRestServiceServer.bindTo(builder).build();

        service = new HealthService(
                builder.build(),
                JsonMapper.builder().build()
        );
    }

    @Test
    void 정상_엔진이면_UP을_반환한다() {
        expectHealth().andRespond(withSuccess(
                "{\"status\":\"UP\"}",
                MediaType.APPLICATION_JSON
        ));

        HealthResponse response = service.check();

        assertTrue(response.isReady());
        assertEquals("UP", response.fastApi());
        server.verify();
    }

    @Test
    void HTTP_200이어도_초기화_중이면_준비되지_않음으로_처리한다() {
        expectHealth().andRespond(withSuccess(
                "{\"status\":\"INITIALIZING\"}",
                MediaType.APPLICATION_JSON
        ));

        assertUnavailable();
    }

    @Test
    void HTTP_503이면_준비되지_않음으로_처리한다() {
        expectHealth().andRespond(
                withStatus(HttpStatus.SERVICE_UNAVAILABLE)
        );

        assertUnavailable();
    }

    @Test
    void 잘못된_JSON이면_준비되지_않음으로_처리한다() {
        expectHealth().andRespond(withSuccess(
                "invalid-json",
                MediaType.APPLICATION_JSON
        ));

        assertUnavailable();
    }

    @Test
    void 시간_초과이면_준비되지_않음으로_처리한다() {
        expectHealth().andRespond(request -> {
            throw new SocketTimeoutException("health timeout");
        });

        assertUnavailable();
    }

    private org.springframework.test.web.client.ResponseActions
            expectHealth() {
        return server.expect(
                requestTo("http://localhost:8000/health")
        ).andExpect(method(HttpMethod.GET));
    }

    private void assertUnavailable() {
        HealthResponse response = service.check();

        assertFalse(response.isReady());
        assertEquals("UP", response.spring());
        assertEquals("UNAVAILABLE", response.fastApi());
        server.verify();
    }
}