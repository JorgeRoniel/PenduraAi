package com.ufc.apiPenduraAi.controllers.user;

import com.ufc.apiPenduraAi.domain.user.User;
import com.ufc.apiPenduraAi.domain.user.UserRoles;
import com.ufc.apiPenduraAi.dtos.user.CreateUserDTO;
import com.ufc.apiPenduraAi.dtos.user.LoginUserDTO;
import com.ufc.apiPenduraAi.dtos.user.ReturnUserDTO;
import com.ufc.apiPenduraAi.services.token.TokenService;
import com.ufc.apiPenduraAi.services.user.UserServices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserControllerTest {

    private final User user = new User(1L, "Ana", "ana@example.com", "encoded-password", UserRoles.USER, null, null);
    private UserController controller;

    @BeforeEach
    void setUp() {
        controller = new UserController(new UserServicesStub(), new TokenServiceStub());
        ReflectionTestUtils.setField(controller, "secureCookie", true);
    }

    @Test
    void loginSetsAccessAndRefreshCookiesWithoutReturningTokens() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        var result = controller.login(new LoginUserDTO("ana@example.com", "123456"), response);

        List<String> cookies = response.getHeaders("Set-Cookie");
        assertEquals(200, result.getStatusCode().value());
        assertEquals("ana@example.com", result.getBody().email());
        assertTrue(cookies.stream().anyMatch(cookie -> cookie.contains("ACCESS_TOKEN=access-token")
                && cookie.contains("Max-Age=900") && cookie.contains("HttpOnly") && cookie.contains("Secure")));
        assertTrue(cookies.stream().anyMatch(cookie -> cookie.contains("REFRESH_TOKEN=refresh-token")
                && cookie.contains("Max-Age=604800") && cookie.contains("Path=/api/user/auth")));
    }

    @Test
    void refreshSetsOnlyANewAccessCookie() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        var result = controller.refresh("refresh-token", response);

        List<String> cookies = response.getHeaders("Set-Cookie");
        assertEquals(204, result.getStatusCode().value());
        assertEquals(1, cookies.size());
        assertTrue(cookies.getFirst().contains("ACCESS_TOKEN=access-token"));
    }

    @Test
    void logoutExpiresBothCookies() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        var result = controller.logout(response);

        List<String> cookies = response.getHeaders("Set-Cookie");
        assertEquals(204, result.getStatusCode().value());
        assertTrue(cookies.stream().anyMatch(cookie -> cookie.contains("ACCESS_TOKEN=") && cookie.contains("Max-Age=0")));
        assertTrue(cookies.stream().anyMatch(cookie -> cookie.contains("REFRESH_TOKEN=") && cookie.contains("Max-Age=0")));
    }

    @Test
    void currentUserReturnsOnlyTheAuthenticatedUserData() {
        var result = controller.currentUser(user);

        assertEquals(200, result.getStatusCode().value());
        assertEquals(user.getEmail(), result.getBody().email());
        assertEquals(user.getId(), result.getBody().id());
    }

    private class UserServicesStub implements UserServices {
        @Override
        public User createUser(CreateUserDTO data) {
            return user;
        }

        @Override
        public User authUser(LoginUserDTO data) {
            return user;
        }

        @Override
        public User findByEmail(String email) {
            return user.getEmail().equals(email) ? user : null;
        }

        @Override
        public Page<ReturnUserDTO> listAllUsers(Pageable pageable) {
            return Page.empty();
        }
    }

    private static class TokenServiceStub implements TokenService {
        @Override
        public String createAccessToken(User user) {
            return "access-token";
        }

        @Override
        public String createRefreshToken(User user) {
            return "refresh-token";
        }

        @Override
        public String verifyAccessToken(String token) {
            return "ana@example.com";
        }

        @Override
        public String verifyRefreshToken(String token) {
            return "ana@example.com";
        }
    }
}
