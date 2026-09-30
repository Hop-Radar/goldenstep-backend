package com.tjoeun.goldenstep.search.handler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.tjoeun.goldenstep.search.service.SearchSessionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SearchSessionCleanupHandler {

    private final SearchSessionService searchSessionService;

    @Scheduled(
            initialDelayString =
                    "${goldenstep.search.cleanup.initial-delay-ms:60000}",
            fixedDelayString =
                    "${goldenstep.search.cleanup.fixed-delay-ms:60000}"
    )
    public void cleanupExpiredSessions() {
        try {
			int deletedCount = searchSessionService.deleteExpiredSessions();

            if (deletedCount > 0) {
                log.info(
                        "만료된 탐색 세션 {}개를 삭제했습니다.",
                        deletedCount
                );
            }
        } catch (Exception exception) {
            log.error(
                    "만료된 탐색 세션 자동 삭제에 실패했습니다. 다음 실행에서 다시 시도합니다.",
                    exception
            );
        }
    }
}