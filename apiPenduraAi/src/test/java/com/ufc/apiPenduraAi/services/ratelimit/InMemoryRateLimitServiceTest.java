package com.ufc.apiPenduraAi.services.ratelimit;

import com.ufc.apiPenduraAi.exceptions.security.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryRateLimitServiceTest {

    private MutableClock clock;
    private InMemoryRateLimitService service;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-10-07T12:00:00Z"));
        service = new InMemoryRateLimitService(clock);
    }

    @Test
    void blocksLoginByAccountEvenWhenRequestsComeFromDifferentAddresses() {
        for (int attempt = 0; attempt < 10; attempt++) {
            service.checkLogin("192.0.2." + attempt, "ana@example.com");
        }

        RateLimitExceededException exception = assertThrows(
                RateLimitExceededException.class,
                () -> service.checkLogin("198.51.100.10", "ANA@example.com")
        );

        assertEquals(900, exception.getRetryAfterSeconds());
    }

    @Test
    void successfulLoginResetsTheAccountCounter() {
        for (int attempt = 0; attempt < 10; attempt++) {
            service.checkLogin("192.0.2." + attempt, "ana@example.com");
        }

        service.resetLoginAttempts("ANA@example.com");

        assertDoesNotThrow(() -> service.checkLogin("198.51.100.10", "ana@example.com"));
    }

    @Test
    void blocksLoginByAddressEvenWhenDifferentAccountsAreUsed() {
        for (int attempt = 0; attempt < 30; attempt++) {
            service.checkLogin("192.0.2.1", "user" + attempt + "@example.com");
        }

        RateLimitExceededException exception = assertThrows(
                RateLimitExceededException.class,
                () -> service.checkLogin("192.0.2.1", "another@example.com")
        );

        assertEquals(900, exception.getRetryAfterSeconds());
    }

    @Test
    void loginWindowExpiresWithoutWaitingInTheTest() {
        for (int attempt = 0; attempt < 10; attempt++) {
            service.checkLogin("192.0.2." + attempt, "ana@example.com");
        }

        clock.advance(Duration.ofMinutes(15));

        assertDoesNotThrow(() -> service.checkLogin("198.51.100.10", "ana@example.com"));
    }

    @Test
    void blocksRegistrationsByAddressAndReturnsRetryTime() {
        for (int attempt = 0; attempt < 5; attempt++) {
            service.checkRegistration("192.0.2.1");
        }

        RateLimitExceededException exception = assertThrows(
                RateLimitExceededException.class,
                () -> service.checkRegistration("192.0.2.1")
        );

        assertEquals(3600, exception.getRetryAfterSeconds());
    }

    @Test
    void blocksRefreshByTokenIndependentlyFromTheAddress() {
        for (int attempt = 0; attempt < 10; attempt++) {
            service.checkRefresh("192.0.2." + attempt, "refresh-token");
        }

        RateLimitExceededException exception = assertThrows(
                RateLimitExceededException.class,
                () -> service.checkRefresh("198.51.100.10", "refresh-token")
        );

        assertTrue(exception.getRetryAfterSeconds() > 0);
        assertTrue(exception.getRetryAfterSeconds() <= 60);
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
