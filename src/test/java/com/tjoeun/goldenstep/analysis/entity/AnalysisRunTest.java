package com.tjoeun.goldenstep.analysis.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.tjoeun.goldenstep.global.exception.ErrorCode;
import com.tjoeun.goldenstep.global.exception.RestException;

class AnalysisRunTest {

    @Test
    void 진행_중인_분석을_실패로_변경한다() {
		LocalDateTime requestedAt = LocalDateTime.of(2026, 9, 30, 12, 0);

        AnalysisRun run = new AnalysisRun(null, requestedAt);

        LocalDateTime failedAt = requestedAt.plusSeconds(10);

        run.fail(failedAt);

        assertAll(
                () -> assertEquals(
                        Status.FAILED,
                        run.getStatus()
                ),
                () -> assertEquals(
                        failedAt,
                        run.getCompletedAt()
                )
        );
    }

    @Test
    void 요청_시각보다_이른_종료_시각은_거부한다() {
		LocalDateTime requestedAt = LocalDateTime.of(2026, 9, 30, 12, 0);

        AnalysisRun run = new AnalysisRun(null, requestedAt);

        RestException exception = assertThrows(
                RestException.class,
                () -> run.fail(requestedAt.minusSeconds(1))
        );

        assertAll(
                () -> assertEquals(
                        ErrorCode.INVALID_ANALYSIS_COMPLETION_TIME,
                        exception.getErrorCode()
                ),
                () -> assertEquals(
                        Status.PROCESSING,
                        run.getStatus()
                ),
                () -> assertNull(run.getCompletedAt())
        );
    }

    @Test
    void 종료_시각이_없으면_거부한다() {
		LocalDateTime requestedAt = LocalDateTime.of(2026, 9, 30, 12, 0);

        AnalysisRun run = new AnalysisRun(null, requestedAt);

        RestException exception = assertThrows(
                RestException.class,
                () -> run.fail(null)
        );

        assertEquals(
                ErrorCode.INVALID_ANALYSIS_COMPLETION_TIME,
                exception.getErrorCode()
        );
    }

    @Test
    void 이미_실패한_분석을_다시_실패_처리하지_않는다() {
		LocalDateTime requestedAt = LocalDateTime.of(2026, 9, 30, 12, 0);

        AnalysisRun run = new AnalysisRun(null, requestedAt);

        LocalDateTime failedAt = requestedAt.plusSeconds(10);
        run.fail(failedAt);

        RestException exception = assertThrows(
                RestException.class,
                () -> run.fail(requestedAt.plusSeconds(20))
        );

        assertAll(
                () -> assertEquals(
                        ErrorCode.INVALID_INITIAL_ANALYSIS_STATUS,
                        exception.getErrorCode()
                ),
                () -> assertEquals(
                        failedAt,
                        run.getCompletedAt()
                )
        );
    }
}