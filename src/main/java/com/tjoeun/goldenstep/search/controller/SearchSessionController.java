package com.tjoeun.goldenstep.search.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tjoeun.goldenstep.search.dto.request.CreateSearchSessionRequest;
import com.tjoeun.goldenstep.search.dto.response.CreateSearchSessionResponse;
import com.tjoeun.goldenstep.search.handler.SearchSessionCookieHandler;
import com.tjoeun.goldenstep.search.service.SearchSessionService;
import com.tjoeun.goldenstep.search.service.SearchSessionService.CreateResult;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchSessionController {

    private final SearchSessionService searchSessionService;
    private final SearchSessionCookieHandler cookieHandler;

    @PostMapping("/input")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateSearchSessionResponse create(
            @Valid @RequestBody CreateSearchSessionRequest request,
            HttpServletResponse response
    ) {
        CreateResult result = searchSessionService.create(request);

        cookieHandler.writeRecoveryCookie(
                response,
                result.getRecoveryToken()
        );

        return new CreateSearchSessionResponse(
                result.getSessionId(),
                result.getExpiresAt()
        );
    }
}