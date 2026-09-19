package com.ufc.apiPenduraAi.services.token.implementation;

import com.ufc.apiPenduraAi.domain.user.User;
import com.ufc.apiPenduraAi.domain.user.UserRoles;
import com.ufc.apiPenduraAi.exceptions.token.InvalidTokenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TokenServiceImplTest {

    private TokenServiceImpl service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new TokenServiceImpl();
        ReflectionTestUtils.setField(service, "secret", "test-secret");
        user = new User(1L, "Ana", "ana@example.com", "encoded-password", UserRoles.USER, null, null);
    }

    @Test
    void validatesAccessTokensOnlyAsAccessTokens() {
        String accessToken = service.createAccessToken(user);
        String refreshToken = service.createRefreshToken(user);

        assertEquals("ana@example.com", service.verifyAccessToken(accessToken));
        assertThrows(InvalidTokenException.class, () -> service.verifyAccessToken(refreshToken));
        assertThrows(InvalidTokenException.class, () -> service.verifyRefreshToken(accessToken));
    }
}
