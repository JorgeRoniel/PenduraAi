package com.ufc.apiPenduraAi.infra;

import com.ufc.apiPenduraAi.exceptions.security.RateLimitExceededException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RestExceptionHandlerTest {

    @Test
    void rateLimitResponseUses429AndRetryAfterHeader() {
        var handler = new RestExceptionHandler();

        var response = handler.rateLimitExceededHandler(new RateLimitExceededException(42));

        assertEquals(429, response.getStatusCode().value());
        assertEquals("42", response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER));
        assertEquals("Muitas tentativas. Tente novamente mais tarde.", response.getBody());
    }
}
