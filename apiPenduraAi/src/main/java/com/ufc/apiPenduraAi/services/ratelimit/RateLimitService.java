package com.ufc.apiPenduraAi.services.ratelimit;

public interface RateLimitService {

    void checkLogin(String clientAddress, String email);

    void resetLoginAttempts(String email);

    void checkRegistration(String clientAddress);

    void checkRefresh(String clientAddress, String refreshToken);
}
