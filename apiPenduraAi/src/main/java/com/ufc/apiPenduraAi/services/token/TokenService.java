package com.ufc.apiPenduraAi.services.token;

import com.ufc.apiPenduraAi.domain.user.User;

public interface TokenService {
    String createAccessToken(User user);
    String verifyAccessToken(String token);
}
