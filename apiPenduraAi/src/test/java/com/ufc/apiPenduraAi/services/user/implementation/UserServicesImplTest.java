package com.ufc.apiPenduraAi.services.user.implementation;

import com.ufc.apiPenduraAi.domain.user.User;
import com.ufc.apiPenduraAi.dtos.user.CreateUserDTO;
import com.ufc.apiPenduraAi.repositories.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServicesImplTest {

    @Mock
    private UserRepository repository;

    @Mock
    private PasswordEncoder encoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private UserServicesImpl services;

    @Test
    void createUserNormalizedEmailBeforeCheckingAndSaving() {
        when(encoder.encode("1234567890")).thenReturn("encoded-password");
        when(repository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        services.createUser(new CreateUserDTO("Ana", "Ana@email.COM", "1234567890"));

        verify(repository).existsByEmail("ana@email.com");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        verify(repository).save(userCaptor.capture());

        assertEquals(
                "ana@email.com",
                userCaptor.getValue().getEmail()
        );
    }
}
