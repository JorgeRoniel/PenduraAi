package com.ufc.apiPenduraAi.services.refresh.implementation;

import com.ufc.apiPenduraAi.domain.refresh.RefreshSession;
import com.ufc.apiPenduraAi.domain.user.User;
import com.ufc.apiPenduraAi.exceptions.token.InvalidTokenException;
import com.ufc.apiPenduraAi.repositories.refresh.RefreshSessionRepository;
import com.ufc.apiPenduraAi.services.refresh.RefreshSessionService;
import com.ufc.apiPenduraAi.services.refresh.RefreshTokenCodec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshServiceImpl implements RefreshSessionService {

    private static final long SESSION_DURATION_DAYS = 7;

    private final RefreshSessionRepository repository;
    private final RefreshTokenCodec tokenCodec;


    @Override
    @Transactional
    public String createSession(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException(
                    "Usuário persistido é obrigatório"
            );
        }

        UUID sessionId = UUID.randomUUID();
        Instant expiration = Instant.now()
                .plus(SESSION_DURATION_DAYS, ChronoUnit.DAYS);

        var issuedToken = tokenCodec.issue(sessionId);

        RefreshSession session = new RefreshSession(
                sessionId,
                user,
                issuedToken.secretHash(),
                expiration
        );

        repository.save(session);

        return issuedToken.value();
    }

    @Override
    @Transactional(noRollbackFor = InvalidTokenException.class)
    public RotationResult rotateSession(String refreshToken) {
        var parsedToken = tokenCodec.parse(refreshToken);
        RefreshSession session = repository
                .findByIdForUpdate(parsedToken.sessionId())
                .orElseThrow(this::invalidToken);

        Instant now = Instant.now();
        if (!session.isActive(now)) {
            session.revoke(now);
            repository.saveAndFlush(session);
            throw invalidToken();
        }

        if (!hashesMatch(session.getTokenHash(), parsedToken.secretHash())) {
            session.revoke(now);
            repository.saveAndFlush(session);
            throw invalidToken();
        }

        var issuedToken = tokenCodec.issue(session.getId());
        session.rotateToken(issuedToken.secretHash());
        repository.saveAndFlush(session);

        return new RotationResult(session.getUser(), issuedToken.value());
    }

    @Override
    @Transactional
    public void revokeSession(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }

        RefreshTokenCodec.ParsedRefreshToken parsedToken;
        try {
            parsedToken = tokenCodec.parse(refreshToken);
        } catch (InvalidTokenException exception) {
            return;
        }

        repository.findByIdForUpdate(parsedToken.sessionId())
                .filter(session -> hashesMatch(session.getTokenHash(), parsedToken.secretHash()))
                .ifPresent(session -> session.revoke(Instant.now()));
    }

    private boolean hashesMatch(String expectedHash, String presentedHash) {
        return MessageDigest.isEqual(
                expectedHash.getBytes(StandardCharsets.US_ASCII),
                presentedHash.getBytes(StandardCharsets.US_ASCII)
        );
    }

    private InvalidTokenException invalidToken() {
        return new InvalidTokenException("Refresh token inválido ou expirado");
    }
}
