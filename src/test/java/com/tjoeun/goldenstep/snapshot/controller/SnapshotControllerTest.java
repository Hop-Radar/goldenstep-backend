package com.tjoeun.goldenstep.snapshot.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.tjoeun.goldenstep.global.exception.ErrorCode;
import com.tjoeun.goldenstep.global.exception.GlobalExceptionHandler;
import com.tjoeun.goldenstep.global.exception.RestException;
import com.tjoeun.goldenstep.snapshot.dto.response.CreateSnapshotResponse;
import com.tjoeun.goldenstep.snapshot.dto.response.SnapshotResponse;
import com.tjoeun.goldenstep.snapshot.dto.response.SnapshotPlaceResponse;
import com.tjoeun.goldenstep.analysis.entity.TimePoint;
import com.tjoeun.goldenstep.snapshot.service.SnapshotService;
import jakarta.servlet.http.Cookie;

class SnapshotControllerTest {
    private SnapshotService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(SnapshotService.class);
        mvc = MockMvcBuilders.standaloneSetup(new SnapshotController(service), new SearchSnapshotController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void 발급은_201과_공유링크를_반환한다() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 10, 4, 12, 0);
        when(service.create(any(), eq("owner"))).thenReturn(new CreateSnapshotResponse(1L,
                "https://example.com/share-map.html#token=abc", now, now.plusHours(12)));
        mvc.perform(post("/api/search/snapshots").cookie(new Cookie("GOLDENSTEP_RECOVERY", "owner"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"runId\":1,\"timePoint\":\"NOW\"}"))
                .andExpect(status().isCreated()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.snapshotId").value(1))
                .andExpect(jsonPath("$.shareUrl").value("https://example.com/share-map.html#token=abc"));
    }

    @Test
    void 공유조회는_프론트의_정보와_지도와_완료상태를_쿠키없이_반환한다() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 10, 4, 12, 0);
        when(service.get("shared-token")).thenReturn(new SnapshotResponse(BigDecimal.valueOf(37.5),
                BigDecimal.valueOf(127), "서울", now.minusHours(1), "파란 옷", TimePoint.AFTER_1H,
                60, Map.of("type", "Polygon"), List.of(new SnapshotPlaceResponse((byte) 1, "장소", "주소",
                BigDecimal.valueOf(37.5), BigDecimal.valueOf(127), true, now.minusMinutes(1))), now, now.plusHours(12)));
        mvc.perform(get("/api/snapshots/shared-token"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.latitude").value(37.5))
                .andExpect(jsonPath("$.additionalInfo").value("파란 옷"))
                .andExpect(jsonPath("$.timePoint").value("AFTER_1H"))
                .andExpect(jsonPath("$.selectedMinutes").value(60))
                .andExpect(jsonPath("$.boundaryZone.type").value("Polygon"))
                .andExpect(jsonPath("$.priorityPlaces[0].checked").value(true))
                .andExpect(jsonPath("$.priorityPlaces[0].name").value("장소"));
    }

    @Test
    void 발급_필수값과_양수_ID를_검증한다() throws Exception {
        for (String body : new String[]{"{}", "{\"runId\":0,\"timePoint\":\"NOW\"}",
                "{\"runId\":1,\"timePoint\":null}"}) {
            mvc.perform(post("/api/search/snapshots").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }

    @Test
    void 알수없는_시간점은_400이다() throws Exception {
        mvc.perform(post("/api/search/snapshots").contentType(MediaType.APPLICATION_JSON)
                .content("{\"runId\":1,\"timePoint\":\"AFTER_2H\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void 조회는_쿠키없이_가능하고_만료오류를_410으로_반환한다() throws Exception {
        when(service.get("expired-token")).thenThrow(new RestException(ErrorCode.SNAPSHOT_UNAVAILABLE));
        mvc.perform(get("/api/snapshots/expired-token"))
                .andExpect(status().isGone()).andExpect(header().string("Cache-Control", "no-store"));
        verify(service).get("expired-token");
    }

    @Test
    void 쿠키가_없으면_발급시_401이다() throws Exception {
        when(service.create(any(), isNull())).thenThrow(new RestException(ErrorCode.RECOVERY_TOKEN_MISSING));
        mvc.perform(post("/api/search/snapshots").contentType(MediaType.APPLICATION_JSON)
                .content("{\"runId\":1,\"timePoint\":\"NOW\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 폐기는_204와_발급자쿠키를_사용한다() throws Exception {
        mvc.perform(delete("/api/search/snapshots/1").cookie(new Cookie("GOLDENSTEP_RECOVERY", "owner")))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(service).revoke(1L, "owner");
    }
}
