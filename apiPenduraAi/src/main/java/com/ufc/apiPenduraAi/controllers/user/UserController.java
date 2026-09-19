package com.ufc.apiPenduraAi.controllers.user;

import com.ufc.apiPenduraAi.dtos.user.CreateUserDTO;
import com.ufc.apiPenduraAi.dtos.user.LoginUserDTO;
import com.ufc.apiPenduraAi.dtos.user.ReturnUserDTO;
import com.ufc.apiPenduraAi.domain.user.User;
import com.ufc.apiPenduraAi.exceptions.token.InvalidTokenException;
import com.ufc.apiPenduraAi.services.token.TokenService;
import com.ufc.apiPenduraAi.services.user.UserServices;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.ResponseCookie;
import org.springframework.beans.factory.annotation.Value;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private static final String ACCESS_COOKIE = "ACCESS_TOKEN";
    private static final String REFRESH_COOKIE = "REFRESH_TOKEN";
    private static final Duration ACCESS_TOKEN_DURATION = Duration.ofMinutes(15);
    private static final Duration REFRESH_TOKEN_DURATION = Duration.ofDays(7);

    private final UserServices services;
    private final TokenService tokenService;

    @Value("${auth.cookie.secure}")
    private boolean secureCookie;

    @PostMapping("/register")
    public ResponseEntity<String> createUser(@RequestBody @Valid CreateUserDTO data) {
        services.createUser(data);
        return ResponseEntity.status(HttpStatus.CREATED).body("Usuário criado com sucesso!");
    }

    @PostMapping("/auth/login")
    public ResponseEntity<ReturnUserDTO> login(@RequestBody @Valid LoginUserDTO data, HttpServletResponse response) {
        User user = services.authUser(data);
        addAuthCookies(response, user);
        return ResponseEntity.ok(toUserDto(user));
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<Void> refresh(
            @CookieValue(value = REFRESH_COOKIE, required = false) String refreshToken,
            HttpServletResponse response
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidTokenException("Token de atualização ausente");
        }

        User user = services.findByEmail(tokenService.verifyRefreshToken(refreshToken));
        if (user == null) {
            throw new InvalidTokenException("Token de atualização inválido");
        }
        response.addHeader("Set-Cookie", createCookie(
                ACCESS_COOKIE, tokenService.createAccessToken(user), "/", ACCESS_TOKEN_DURATION).toString());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        response.addHeader("Set-Cookie", createCookie(ACCESS_COOKIE, "", "/", Duration.ZERO).toString());
        response.addHeader("Set-Cookie", createCookie(REFRESH_COOKIE, "", "/api/user/auth", Duration.ZERO).toString());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<ReturnUserDTO> currentUser(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(toUserDto(user));
    }

    @GetMapping
    public ResponseEntity<Page<ReturnUserDTO>> listUsers(
            @PageableDefault(size = 10, sort = "nome") Pageable pageable
    ) {
        return ResponseEntity.ok(services.listAllUsers(pageable));
    }

    private void addAuthCookies(HttpServletResponse response, User user) {
        response.addHeader("Set-Cookie", createCookie(
                ACCESS_COOKIE, tokenService.createAccessToken(user), "/", ACCESS_TOKEN_DURATION).toString());
        response.addHeader("Set-Cookie", createCookie(
                REFRESH_COOKIE, tokenService.createRefreshToken(user), "/api/user/auth", REFRESH_TOKEN_DURATION).toString());
    }

    private ResponseCookie createCookie(String name, String value, String path, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path(path)
                .maxAge(maxAge)
                .build();
    }

    private ReturnUserDTO toUserDto(User user) {
        return new ReturnUserDTO(user.getId(), user.getNome(), user.getEmail(), user.getRole().name(), user.getCreatedAt());
    }
}
