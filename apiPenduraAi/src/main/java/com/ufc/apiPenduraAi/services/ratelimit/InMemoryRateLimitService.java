package com.ufc.apiPenduraAi.services.ratelimit;

import com.ufc.apiPenduraAi.exceptions.security.RateLimitExceededException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class InMemoryRateLimitService implements RateLimitService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryRateLimitService.class);

    private static final int LOGIN_IP_LIMIT = 30;
    private static final Duration LOGIN_IP_WINDOW = Duration.ofMinutes(15);
    private static final int LOGIN_ACCOUNT_LIMIT = 10;
    private static final Duration LOGIN_ACCOUNT_WINDOW = Duration.ofMinutes(15);
    private static final int REGISTRATION_IP_LIMIT = 5;
    private static final Duration REGISTRATION_IP_WINDOW = Duration.ofHours(1);
    private static final int REFRESH_IP_LIMIT = 120;
    private static final Duration REFRESH_IP_WINDOW = Duration.ofMinutes(15);
    private static final int REFRESH_TOKEN_LIMIT = 10;
    private static final Duration REFRESH_TOKEN_WINDOW = Duration.ofMinutes(1);

    private static final String UNKNOWN_CLIENT = "unknown-client";
    private static final String MISSING_TOKEN = "missing-token";

    private final ConcurrentHashMap<String, AttemptWindow> windows = new ConcurrentHashMap<>();
    private final Clock clock;

    @Autowired
    public InMemoryRateLimitService() {
        this(Clock.systemUTC());
    }

    InMemoryRateLimitService(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void checkLogin(String clientAddress, String email) {
        acquire("login-ip", identifier(clientAddress, UNKNOWN_CLIENT), LOGIN_IP_LIMIT, LOGIN_IP_WINDOW);
        acquire("login-account", normalizedEmail(email), LOGIN_ACCOUNT_LIMIT, LOGIN_ACCOUNT_WINDOW);
    }

    @Override
    public void resetLoginAttempts(String email) {
        windows.remove(key("login-account", normalizedEmail(email)));
    }

    @Override
    public void checkRegistration(String clientAddress) {
        acquire("registration-ip", identifier(clientAddress, UNKNOWN_CLIENT),
                REGISTRATION_IP_LIMIT, REGISTRATION_IP_WINDOW);
    }

    @Override
    public void checkRefresh(String clientAddress, String refreshToken) {
        acquire("refresh-ip", identifier(clientAddress, UNKNOWN_CLIENT), REFRESH_IP_LIMIT, REFRESH_IP_WINDOW);
        acquire("refresh-token", identifier(refreshToken, MISSING_TOKEN), REFRESH_TOKEN_LIMIT, REFRESH_TOKEN_WINDOW);
    }

    private void acquire(String scope, String identifier, int limit, Duration duration) {
        long now = clock.millis();
        AtomicLong retryAfterMillis = new AtomicLong();
        String key = key(scope, identifier);

        windows.compute(key, (ignored, current) -> {
            AttemptWindow window = current == null ? new AttemptWindow(duration) : current;
            removeExpired(window, now);

            if (window.attempts.size() >= limit) {
                long firstAttempt = window.attempts.getFirst();
                retryAfterMillis.set(Math.max(1, firstAttempt + duration.toMillis() - now));
                return window;
            }

            window.attempts.addLast(now);
            return window;
        });

        if (retryAfterMillis.get() > 0) {
            long retryAfterSeconds = Math.max(1, (retryAfterMillis.get() + 999) / 1000);
            LOGGER.warn("Rate limit excedido: escopo={}, retryAfterSeconds={}", scope, retryAfterSeconds);
            throw new RateLimitExceededException(retryAfterSeconds);
        }
    }

    private String key(String scope, String identifier) {
        return scope + ':' + sha256(identifier);
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível", exception);
        }
    }

    private String identifier(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private String normalizedEmail(String email) {
        return identifier(email, "missing-email").toLowerCase(java.util.Locale.ROOT);
    }

    private void removeExpired(AttemptWindow window, long now) {
        long cutoff = now - window.duration.toMillis();
        while (!window.attempts.isEmpty() && window.attempts.getFirst() <= cutoff) {
            window.attempts.removeFirst();
        }
    }

    @Scheduled(fixedDelayString = "${app.rate-limit.cleanup-ms:600000}")
    void cleanupExpiredWindows() {
        long now = clock.millis();
        windows.forEach((key, ignored) -> windows.computeIfPresent(key, (currentKey, window) -> {
            removeExpired(window, now);
            return window.attempts.isEmpty() ? null : window;
        }));
    }

    private static final class AttemptWindow {
        private final Duration duration;
        private final ArrayDeque<Long> attempts = new ArrayDeque<>();

        private AttemptWindow(Duration duration) {
            this.duration = duration;
        }
    }
}
