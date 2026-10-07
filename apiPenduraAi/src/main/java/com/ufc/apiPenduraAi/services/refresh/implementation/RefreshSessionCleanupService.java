package com.ufc.apiPenduraAi.services.refresh.implementation;

import com.ufc.apiPenduraAi.repositories.refresh.RefreshSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class RefreshSessionCleanupService {

    private static final long REVOKED_SESSION_RETENTION_DAYS = 30;

    private final RefreshSessionRepository repository;

    @Scheduled(
            cron = "${app.refresh-session.cleanup-cron:0 0 3 * * *}",
            zone = "UTC"
    )
    @Transactional
    public int deleteObsoleteSessions() {
        Instant now = Instant.now();
        Instant revokedBefore = now.minus(
                REVOKED_SESSION_RETENTION_DAYS,
                ChronoUnit.DAYS
        );

        return repository.deleteObsoleteSessions(now, revokedBefore);
    }
}
