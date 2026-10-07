package com.ufc.apiPenduraAi.services.refresh.implementation;

import com.ufc.apiPenduraAi.exceptions.token.InvalidTokenException;
import com.ufc.apiPenduraAi.services.refresh.RefreshTokenCodec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class RefreshTokenCodecTest {

    private RefreshTokenCodec codec;

    @BeforeEach
    void setUp() {
        codec = new RefreshTokenCodec();
    }

    @Test
    void generatesAndParsesRefreshToken() {
        UUID sessionId = UUID.randomUUID();

        var issuedToken = codec.issue(sessionId);
        var parsedToken = codec.parse(issuedToken.value());

        assertTrue(issuedToken.value().startsWith(sessionId + "."));
        assertEquals(64, issuedToken.secretHash().length());

        assertEquals(sessionId, parsedToken.sessionId());
        assertEquals(issuedToken.secretHash(), parsedToken.secretHash());

        assertFalse(issuedToken.value().contains(
                issuedToken.secretHash()
        ));
    }

    @Test
    void generatesDifferentSecretsForTheSameSession() {
        UUID sessionId = UUID.randomUUID();

        var firstToken = codec.issue(sessionId);
        var secondToken = codec.issue(sessionId);

        assertEquals(firstToken.sessionId(), secondToken.sessionId());
        assertNotEquals(firstToken.value(), secondToken.value());
        assertNotEquals(firstToken.secretHash(), secondToken.secretHash());
    }

    @Test
    void rejectsMalformedRefreshTokens() {
        assertThrows(
                InvalidTokenException.class,
                () -> codec.parse(null)
        );

        assertThrows(
                InvalidTokenException.class,
                () -> codec.parse("")
        );

        assertThrows(
                InvalidTokenException.class,
                () -> codec.parse("invalid-token")
        );

        assertThrows(
                InvalidTokenException.class,
                () -> codec.parse("invalid-uuid.secret")
        );

        assertThrows(
                InvalidTokenException.class,
                () -> codec.parse(UUID.randomUUID() + ".short")
        );
    }
}
