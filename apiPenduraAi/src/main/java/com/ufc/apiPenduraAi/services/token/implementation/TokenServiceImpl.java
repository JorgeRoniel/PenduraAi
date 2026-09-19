package com.ufc.apiPenduraAi.services.token.implementation;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.ufc.apiPenduraAi.domain.user.User;
import com.ufc.apiPenduraAi.exceptions.token.InvalidTokenException;
import com.ufc.apiPenduraAi.services.token.TokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class TokenServiceImpl implements TokenService {

    private static final String ISSUER = "api_pendura_ai";
    private static final String TOKEN_TYPE_CLAIM = "token_type";

    @Value("${jwt.secret}")
    private String secret;

    @Override
    public String createAccessToken(User user) {
        return createToken(user, "access", 15, ChronoUnit.MINUTES);
    }

    @Override
    public String createRefreshToken(User user) {
        return createToken(user, "refresh", 7, ChronoUnit.DAYS);
    }

    @Override
    public String verifyAccessToken(String token) {
        return verifyToken(token, "access");
    }

    @Override
    public String verifyRefreshToken(String token) {
        return verifyToken(token, "refresh");
    }

    private String createToken(User user, String tokenType, long expirationAmount, ChronoUnit expirationUnit) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withIssuer(ISSUER)
                    .withSubject(user.getEmail())
                    .withClaim(TOKEN_TYPE_CLAIM, tokenType)
                    .withExpiresAt(generateExpirationTime(expirationAmount, expirationUnit))
                    .sign(algorithm);
        } catch (JWTCreationException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private String verifyToken(String token, String expectedTokenType) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.require(algorithm)
                    .withIssuer(ISSUER)
                    .withClaim(TOKEN_TYPE_CLAIM, expectedTokenType)
                    .build()
                    .verify(token)
                    .getSubject();
        } catch (JWTVerificationException e) {
            throw new InvalidTokenException("Token inválido ou expirado");
        }
    }

    private Instant generateExpirationTime(long amount, ChronoUnit unit) {
        return Instant.now().plus(amount, unit);
    }
}
