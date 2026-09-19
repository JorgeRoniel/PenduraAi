package com.ufc.apiPenduraAi.controllers.user;

import com.ufc.apiPenduraAi.dtos.user.CreateUserDTO;
import com.ufc.apiPenduraAi.dtos.user.LoginUserDTO;
import com.ufc.apiPenduraAi.dtos.user.ReturnLoginDTO;
import com.ufc.apiPenduraAi.dtos.user.ReturnUserDTO;
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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.http.ResponseCookie;
import org.springframework.beans.factory.annotation.Value;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserServices services;

    @Value("${auth.cookie.secure:true}")
    private boolean secureCookie;

    @PostMapping("/register")
    public ResponseEntity<String> createUser(@RequestBody @Valid CreateUserDTO data) {
        services.createUser(data);
        return ResponseEntity.status(HttpStatus.CREATED).body("Usuário criado com sucesso!");
    }

    @PostMapping("/auth/login")
    public ResponseEntity<ReturnLoginDTO> login(@RequestBody @Valid LoginUserDTO data, HttpServletResponse response) {
        ReturnLoginDTO login = services.authUser(data);
        ResponseCookie cookie = ResponseCookie.from("AUTH_TOKEN", login.token())
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofHours(1))
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
        return ResponseEntity.status(HttpStatus.OK).body(login);
    }

    @GetMapping("/me")
    public ResponseEntity<ReturnUserDTO> currentUser(@AuthenticationPrincipal UserDetails principal) {
        com.ufc.apiPenduraAi.domain.user.User user = (com.ufc.apiPenduraAi.domain.user.User) principal;
        return ResponseEntity.ok(new ReturnUserDTO(
                user.getId(), user.getNome(), user.getEmail(), user.getRole().name(), user.getCreatedAt()));
    }

    @GetMapping
    public ResponseEntity<Page<ReturnUserDTO>> listUsers(
            @PageableDefault(size = 10, sort = "nome") Pageable pageable
    ) {
        return ResponseEntity.ok(services.listAllUsers(pageable));
    }
}
