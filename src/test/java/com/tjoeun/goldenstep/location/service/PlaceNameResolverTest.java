package com.tjoeun.goldenstep.location.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.tjoeun.goldenstep.global.exception.ErrorCode;
import com.tjoeun.goldenstep.global.exception.RestException;

import tools.jackson.databind.json.JsonMapper;

class PlaceNameResolverTest {

    private static final String ADDRESS =
            "서울특별시 중구 소공로 65-1";

    private static final BigDecimal LAT =
            new BigDecimal("37.5608337");

    private static final BigDecimal LNG =
            new BigDecimal("126.9816154");

    private MockRestServiceServer server;
    private PlaceNameResolver resolver;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://maps.apigw.ntruss.com");

        server = MockRestServiceServer.bindTo(builder).build();

        resolver = new PlaceNameResolver(
                builder.build(),
                JsonMapper.builder().build()
        );
    }

    @Test
    void 정상_이름은_API_호출_없이_유지한다() {
        assertEquals(
                "신세계백화점",
                resolver.resolve(
                        "신세계백화점",
                        ADDRESS,
                        LAT,
                        LNG
                )
        );

        server.verify();
    }

    @Test
    void 내부_코드를_건물명으로_보정한다() {
        server.expect(this::verifyRequest)
                .andRespond(withSuccess(
                        buildingResponse("types", "신세계백화점"),
                        MediaType.APPLICATION_JSON
                ));

        assertEquals(
                "신세계백화점",
                resolver.resolve(
                        "1531 B001000000TIQNL",
                        ADDRESS,
                        LAT,
                        LNG
                )
        );

        server.verify();
    }

    @Test
    void type_배열도_지원한다() {
        server.expect(this::verifyRequest)
                .andRespond(withSuccess(
                        buildingResponse("type", "신세계백화점"),
                        MediaType.APPLICATION_JSON
                ));

        assertEquals(
                "신세계백화점",
                resolver.resolve("unknown", ADDRESS, LAT, LNG)
        );

        server.verify();
    }

    @Test
    void 건물명이_없으면_주소를_사용한다() {
        server.expect(this::verifyRequest)
                .andRespond(withSuccess(
                        buildingResponse("types", ""),
                        MediaType.APPLICATION_JSON
                ));

        assertEquals(
                ADDRESS,
                resolver.resolve(
                        "B001000000BICVZE",
                        ADDRESS,
                        LAT,
                        LNG
                )
        );

        server.verify();
    }

    @Test
    void 검색_결과가_없으면_주소를_사용한다() {
        server.expect(this::verifyRequest)
                .andRespond(withSuccess(
                        """
                        {"status":"OK","addresses":[]}
                        """,
                        MediaType.APPLICATION_JSON
                ));

        assertEquals(
                ADDRESS,
                resolver.resolve(null, ADDRESS, LAT, LNG)
        );

        server.verify();
    }

    @Test
    void HTTP_오류는_주소로_복구한다() {
        server.expect(this::verifyRequest)
                .andRespond(
                        withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                );

        assertEquals(
                ADDRESS,
                resolver.resolve("unknown", ADDRESS, LAT, LNG)
        );

        server.verify();
    }

    @Test
    void 연결_오류는_주소로_복구한다() {
        server.expect(this::verifyRequest)
                .andRespond(
                        withException(new IOException("connection failed"))
                );

        assertEquals(
                ADDRESS,
                resolver.resolve("unknown", ADDRESS, LAT, LNG)
        );

        server.verify();
    }

    @Test
    void JSON_파싱_오류는_주소로_복구한다() {
        server.expect(this::verifyRequest)
                .andRespond(withSuccess(
                        "{broken",
                        MediaType.APPLICATION_JSON
                ));

        assertEquals(
                ADDRESS,
                resolver.resolve("unknown", ADDRESS, LAT, LNG)
        );

        server.verify();
    }

    @Test
    void 응답_구조_오류는_주소로_복구한다() {
        server.expect(this::verifyRequest)
                .andRespond(withSuccess(
                        """
                        {"status":"OK","addresses":{}}
                        """,
                        MediaType.APPLICATION_JSON
                ));

        assertEquals(
                ADDRESS,
                resolver.resolve("unknown", ADDRESS, LAT, LNG)
        );

        server.verify();
    }

    @Test
    void 좌표가_누락되면_기존_분석_오류를_던진다() {
        RestException exception = assertThrows(
                RestException.class,
                () -> resolver.resolve(
                        "unknown",
                        ADDRESS,
                        null,
                        LNG
                )
        );

        assertEquals(
                ErrorCode.INVALID_ANALYSIS_RESPONSE,
                exception.getErrorCode()
        );

        server.verify();
    }

    @Test
    void 좌표_범위가_잘못되면_기존_분석_오류를_던진다() {
        RestException exception = assertThrows(
                RestException.class,
                () -> resolver.resolve(
                        "unknown",
                        ADDRESS,
                        new BigDecimal("91"),
                        LNG
                )
        );

        assertEquals(
                ErrorCode.INVALID_ANALYSIS_RESPONSE,
                exception.getErrorCode()
        );

        server.verify();
    }

    @Test
    void 주소도_없으면_좌표를_표시한다() {
        assertEquals(
                "위도 37.5608337, 경도 126.9816154",
                resolver.resolve("unknown", null, LAT, LNG)
        );

        server.verify();
    }

    private void verifyRequest(ClientHttpRequest request) {
        assertEquals(HttpMethod.GET, request.getMethod());

        assertEquals(
                "/map-geocode/v2/geocode",
                request.getURI().getPath()
        );

        // 변경: decode() 제거 및 쿼리 파라미터 타입 명시
        MultiValueMap<String, String> params = UriComponentsBuilder
                .fromUri(request.getURI())
                .build()
                .getQueryParams();

        // 변경: 파라미터 값을 개별 디코딩한 후 비교
        assertEquals(
                ADDRESS,
                decodeQueryParameter(params, "query")
        );

        assertEquals(
                LNG.toPlainString() + "," + LAT.toPlainString(),
                decodeQueryParameter(params, "coordinate")
        );

        assertEquals(
                "1",
                decodeQueryParameter(params, "count")
        );
    }

    // 추가: 필수 쿼리 파라미터 존재 여부 검증 및 UTF-8 디코딩
    private String decodeQueryParameter(
            MultiValueMap<String, String> params,
            String parameterName
    ) {
        String value = params.getFirst(parameterName);

        assertNotNull(
                value,
                "필수 쿼리 파라미터가 없습니다: " + parameterName
        );

        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private String buildingResponse(
            String typeField,
            String buildingName
    ) {
        return """
                {
                  "status": "OK",
                  "addresses": [
                    {
                      "addressElements": [
                        {
                          "%s": ["BUILDING_NAME"],
                          "longName": "%s"
                        }
                      ]
                    }
                  ]
                }
                """.formatted(typeField, buildingName);
    }
}