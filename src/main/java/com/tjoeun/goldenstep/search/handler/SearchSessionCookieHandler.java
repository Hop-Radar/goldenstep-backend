package com.tjoeun.goldenstep.search.handler;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletResponse;

@Component
public class SearchSessionCookieHandler {

    private static final String COOKIE_NAME = "GOLDENSTEP_RECOVERY";
    private static final Duration COOKIE_MAX_AGE = Duration.ofHours(24);

    private final boolean secure;

    public SearchSessionCookieHandler(
            @Value("${goldenstep.cookie.secure:false}") boolean secure
    ) {
        this.secure = secure;
    }

    public void writeRecoveryCookie(
            HttpServletResponse response,
            String recoveryToken
    ) {
        ResponseCookie cookie = ResponseCookie.from(
                        COOKIE_NAME,
                        recoveryToken
                )
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(COOKIE_MAX_AGE)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}