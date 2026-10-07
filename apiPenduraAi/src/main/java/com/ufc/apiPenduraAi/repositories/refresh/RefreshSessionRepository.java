package com.ufc.apiPenduraAi.repositories.refresh;

import com.ufc.apiPenduraAi.domain.refresh.RefreshSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.time.Instant;

@Repository
public interface RefreshSessionRepository extends JpaRepository<RefreshSession, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT session
            FROM RefreshSession session
            JOIN FETCH session.user
            WHERE session.id = :id
            """)
    Optional<RefreshSession> findByIdForUpdate(@Param("id") UUID id);

    @Modifying
    @Query("""
            DELETE FROM RefreshSession session
            WHERE session.expiresAt <= :now
               OR (session.revokedAt IS NOT NULL AND session.revokedAt <= :revokedBefore)
            """)
    int deleteObsoleteSessions(
            @Param("now") Instant now,
            @Param("revokedBefore") Instant revokedBefore
    );
}
