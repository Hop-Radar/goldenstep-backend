package com.tjoeun.goldenstep.search.dto.response;

import java.time.LocalDateTime;

import lombok.Getter;

@Getter
public class CreateSearchSessionResponse {

    private final Long sessionId;
    private final LocalDateTime expiresAt;

    public CreateSearchSessionResponse(Long sessionId, LocalDateTime expiresAt) {
        this.sessionId = sessionId;
        this.expiresAt = expiresAt;
    }
}