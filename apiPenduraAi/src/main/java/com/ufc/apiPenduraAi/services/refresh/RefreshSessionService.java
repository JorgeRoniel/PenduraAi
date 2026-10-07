package com.ufc.apiPenduraAi.services.refresh;

import com.ufc.apiPenduraAi.domain.user.User;

public interface RefreshSessionService {

    String createSession(User user);

    RotationResult rotateSession(String refreshToken);

    void revokeSession(String refreshToken);

    record RotationResult(User user, String refreshToken) {
    }
}
