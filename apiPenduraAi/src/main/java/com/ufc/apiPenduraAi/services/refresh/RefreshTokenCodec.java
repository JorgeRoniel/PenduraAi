package com.ufc.apiPenduraAi.services.refresh;

import com.ufc.apiPenduraAi.exceptions.token.InvalidTokenException;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Component
public class RefreshTokenCodec {

    private static final int SECRET_SIZE_BYTES = 32;
    private static final int ENCODED_SECRET_LENGTH = 43;

    private final SecureRandom secureRandom = new SecureRandom();

    public IssuedRefreshToken issue (UUID sessionId) {
        if (sessionId == null){
            throw new IllegalArgumentException("O identificador da sessão é obrigatório");
        }

        byte[] secret = new byte[SECRET_SIZE_BYTES];
        secureRandom.nextBytes(secret);

        String encodedSecret = Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
        String value = sessionId + "." + encodedSecret;
        String secretHash = sha256(secret);

        return new IssuedRefreshToken (sessionId, value, secretHash);
    }

    public ParsedRefreshToken parse(String token) {
        if(token == null || token.isBlank()){
            throw invalidToken();
        }

        String[] parts = token.split("\\.", -1);
        if(parts.length != 2){
            throw invalidToken();
        }

        UUID sessionId = parseSessionId(parts[0]);
        byte[] secret = decodeSecret(parts[1]);

        return new ParsedRefreshToken(sessionId, sha256(secret));
    }

    private UUID parseSessionId(String value) {
        try {
            UUID sessionId = UUID.fromString(value);

            if (!sessionId.toString().equals(value)) {
                throw invalidToken();
            }

            return sessionId;
        } catch (IllegalArgumentException exception) {
            throw invalidToken();
        }
    }

    private byte[] decodeSecret(String value) {
        if (value.length() != ENCODED_SECRET_LENGTH
                || !value.matches("[A-Za-z0-9_-]+")) {
            throw invalidToken();
        }

        try {
            byte[] secret = Base64.getUrlDecoder().decode(value);

            if (secret.length != SECRET_SIZE_BYTES) {
                throw invalidToken();
            }

            return secret;
        } catch (IllegalArgumentException exception) {
            throw invalidToken();
        }
    }

    private String sha256(byte[] value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 não está disponível",
                    exception
            );
        }
    }

    private InvalidTokenException invalidToken() {
        return new InvalidTokenException(
                "Refresh token inválido"
        );
    }

    public record IssuedRefreshToken(
            UUID sessionId,
            String value,
            String secretHash
    ) {
    }

    public record ParsedRefreshToken(
            UUID sessionId,
            String secretHash
    ) {
    }
}
