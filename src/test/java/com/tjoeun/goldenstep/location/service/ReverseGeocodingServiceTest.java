package com.tjoeun.goldenstep.location.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.tjoeun.goldenstep.global.exception.ErrorCode;
import com.tjoeun.goldenstep.global.exception.RestException;

import tools.jackson.databind.json.JsonMapper;

class ReverseGeocodingServiceTest {

	private static final BigDecimal LAT = new BigDecimal("37.566535");
	private static final BigDecimal LNG = new BigDecimal("126.977969");

    private MockRestServiceServer server;
    private ReverseGeocodingService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://maps.apigw.ntruss.com")
                .defaultHeader(
                        "X-NCP-APIGW-API-KEY-ID",
                        "test-client-id"
                )
                .defaultHeader(
                        "X-NCP-APIGW-API-KEY",
                        "test-client-secret"
                );

        server = MockRestServiceServer.bindTo(builder).build();

        service = new ReverseGeocodingService(
                builder.build(),
                JsonMapper.builder().build()
        );
    }

    @Test
    void 도로명_주소를_지번_주소보다_우선한다() {
        expectJson("""
                {
                  "status": {"code": 0},
                  "results": [
                    {
                      "name": "addr",
                      "region": {
                        "area1": {"name": "서울특별시"},
                        "area2": {"name": "중구"},
                        "area3": {"name": "태평로1가"},
                        "area4": {"name": ""}
                      },
                      "land": {
                        "type": "1",
                        "number1": "31",
                        "number2": ""
                      }
                    },
                    {
                      "name": "roadaddr",
                      "region": {
                        "area1": {"name": "서울특별시"},
                        "area2": {"name": "중구"},
                        "area3": {"name": "태평로1가"}
                      },
                      "land": {
                        "name": "세종대로",
                        "number1": "110",
                        "number2": ""
                      }
                    }
                  ]
                }
                """);

        assertEquals(
                "서울특별시 중구 세종대로 110",
                service.findAddress(LAT, LNG)
        );

        server.verify();
    }

    @Test
    void 도로명_주소가_없으면_지번_주소를_반환한다() {
        expectJson("""
                {
                  "status": {"code": 0},
                  "results": [
                    {
                      "name": "addr",
                      "region": {
                        "area1": {"name": "서울특별시"},
                        "area2": {"name": "중구"},
                        "area3": {"name": "태평로1가"},
                        "area4": {"name": ""}
                      },
                      "land": {
                        "type": "1",
                        "number1": "31",
                        "number2": "2"
                      }
                    }
                  ]
                }
                """);

        assertEquals(
                "서울특별시 중구 태평로1가 31-2",
                service.findAddress(LAT, LNG)
        );

        server.verify();
    }

    @Test
    void 불완전한_도로명_주소_대신_지번_주소를_반환한다() {
        expectJson("""
                {
                  "status": {"code": 0},
                  "results": [
                    {
                      "name": "roadaddr",
                      "region": {
                        "area1": {"name": "서울특별시"},
                        "area2": {"name": "중구"}
                      },
                      "land": {
                        "name": null,
                        "number1": "110"
                      }
                    },
                    {
                      "name": "addr",
                      "region": {
                        "area1": {"name": "서울특별시"},
                        "area2": {"name": "중구"},
                        "area3": {"name": "태평로1가"}
                      },
                      "land": {
                        "type": "1",
                        "number1": "31",
                        "number2": ""
                      }
                    }
                  ]
                }
                """);

        assertEquals(
                "서울특별시 중구 태평로1가 31",
                service.findAddress(LAT, LNG)
        );

        server.verify();
    }

    @Test
    void 도로명_주소에_읍면을_포함한다() {
        expectJson("""
                {
                  "status": {"code": 0},
                  "results": [
                    {
                      "name": "roadaddr",
                      "region": {
                        "area1": {"name": "전라남도"},
                        "area2": {"name": "광양시"},
                        "area3": {"name": "광양읍"},
                        "area4": {"name": "읍내리"}
                      },
                      "land": {
                        "name": "매일시장길",
                        "number1": "20",
                        "number2": ""
                      }
                    }
                  ]
                }
                """);

        assertEquals(
                "전라남도 광양시 광양읍 매일시장길 20",
                service.findAddress(LAT, LNG)
        );

        server.verify();
    }

    @Test
    void 주소_결과가_없으면_null을_반환한다() {
        expectJson("""
                {
                  "status": {"code": 3},
                  "results": []
                }
                """);

        assertNull(service.findAddress(LAT, LNG));

        server.verify();
    }

    @Test
    void 네이버_응답_코드가_실패이면_null을_반환한다() {
        expectJson("""
                {
                  "status": {"code": 900},
                  "results": []
                }
                """);

        assertNull(service.findAddress(LAT, LNG));

        server.verify();
    }

    @Test
    void 네이버_HTTP_오류가_발생하면_null을_반환한다() {
        server.expect(this::verifyRequest)
				.andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        assertNull(service.findAddress(LAT, LNG));

        server.verify();
    }

    @Test
    void 응답이_JSON이_아니면_null을_반환한다() {
        server.expect(this::verifyRequest)
                .andRespond(
                        withSuccess(
                                "올바른 JSON이 아닌 응답",
                                MediaType.TEXT_PLAIN
                        )
                );

        assertNull(service.findAddress(LAT, LNG));

        server.verify();
    }

    @Test
    void 좌표가_누락되면_RestException이_발생한다() {
        RestException missingLat = assertThrows(
                RestException.class,
                () -> service.findAddress(null, LNG)
        );

        RestException missingLng = assertThrows(
                RestException.class,
                () -> service.findAddress(LAT, null)
        );

        assertAll(
                () -> assertEquals(
                        ErrorCode.ADDRESS_COORDINATES_MISSING,
                        missingLat.getErrorCode()
                ),
                () -> assertEquals(
                        ErrorCode.ADDRESS_COORDINATES_MISSING,
                        missingLng.getErrorCode()
                )
        );

        // HTTP 요청을 등록하지 않았으므로
        // 외부 요청이 발생했다면 테스트가 실패합니다.
        server.verify();
    }

    @Test
    void 좌표가_범위를_벗어나면_RestException이_발생한다() {
        RestException invalidLat = assertThrows(
                RestException.class,
                () -> service.findAddress(
                        new BigDecimal("90.000001"),
                        LNG
                )
        );

        RestException invalidLng = assertThrows(
                RestException.class,
                () -> service.findAddress(
                        LAT,
                        new BigDecimal("-180.000001")
                )
        );

        assertAll(
                () -> assertEquals(
                        ErrorCode.INVALID_ADDRESS_COORDINATES,
                        invalidLat.getErrorCode()
                ),
                () -> assertEquals(
                        ErrorCode.INVALID_ADDRESS_COORDINATES,
                        invalidLng.getErrorCode()
                )
        );

        server.verify();
    }

    private void expectJson(String body) {
        server.expect(this::verifyRequest)
                .andRespond(
                        withSuccess(
                                body,
                                MediaType.APPLICATION_JSON
                        )
                );
    }

    private void verifyRequest(
            org.springframework.http.client.ClientHttpRequest request
    ) {
        String query = request.getURI().getQuery();

        assertAll(
                () -> assertEquals(
                        HttpMethod.GET,
                        request.getMethod()
                ),
                () -> assertEquals(
                        "/map-reversegeocode/v2/gc",
                        request.getURI().getPath()
                ),
                () -> assertNotNull(query)
        );

        assertAll(
                () -> assertTrue(
                        query.contains(
                                "coords=126.977969,37.566535"
                        )
                ),
                () -> assertTrue(
                        query.contains("sourcecrs=epsg:4326")
                ),
                () -> assertTrue(
                        query.contains("orders=roadaddr,addr")
                ),
                () -> assertTrue(
                        query.contains("output=json")
                ),
                () -> assertEquals(
                        "test-client-id",
                        request.getHeaders().getFirst(
                                "X-NCP-APIGW-API-KEY-ID"
                        )
                ),
                () -> assertEquals(
                        "test-client-secret",
                        request.getHeaders().getFirst(
                                "X-NCP-APIGW-API-KEY"
                        )
                )
        );
    }
}