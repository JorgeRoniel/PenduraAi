package com.ufc.apiPenduraAi.services.divida.implementation;

import com.ufc.apiPenduraAi.domain.divida.Divida;
import com.ufc.apiPenduraAi.domain.user.User;
import com.ufc.apiPenduraAi.domain.user.UserRoles;
import com.ufc.apiPenduraAi.dtos.divida.CreateDividaDTO;
import com.ufc.apiPenduraAi.repositories.divida.DividaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class DividaServicesImplTest {

    @Mock
    private DividaRepository dividaRepository;

    @InjectMocks
    private DividaServicesImpl services;

    @AfterEach
    void tearDown(){
        SecurityContextHolder.clearContext();
    }

    @Test
    void addDebtUsesAuthenticatedPrincipal(){
        User authenticatedUser = new User(
                1L,
                "User",
                "user@mail.com",
                "encoded-password",
                UserRoles.USER,
                null,
                null
        );

        var auth = new UsernamePasswordAuthenticationToken(
                authenticatedUser,
                null,
                authenticatedUser.getAuthorities()
        );

        SecurityContextHolder.getContext().setAuthentication(auth);

        services.addDivida(
                new CreateDividaDTO(
                        "Cliente",
                        new BigDecimal("10.01")
                )
        );

        ArgumentCaptor<Divida> captor = ArgumentCaptor.forClass(Divida.class);
        verify(dividaRepository).save(captor.capture());

        assertSame(
                authenticatedUser,
                captor.getValue().getUser()
        );
    }
}
