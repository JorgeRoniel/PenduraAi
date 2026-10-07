package com.ufc.apiPenduraAi.services.user.implementation;

import com.ufc.apiPenduraAi.domain.user.User;
import com.ufc.apiPenduraAi.domain.user.UserRoles;
import com.ufc.apiPenduraAi.dtos.user.CreateUserDTO;
import com.ufc.apiPenduraAi.dtos.user.LoginUserDTO;
import com.ufc.apiPenduraAi.dtos.user.ReturnUserDTO;
import com.ufc.apiPenduraAi.repositories.user.UserRepository;
import com.ufc.apiPenduraAi.services.user.UserServices;
import com.ufc.apiPenduraAi.utils.EmailNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServicesImpl implements UserServices {

    private final UserRepository repository;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authenticationManager;

    @Override
    public User createUser(CreateUserDTO data) {

        String normalizedEmail = EmailNormalizer.normalized(data.email());

        if (repository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Email já cadastrado!");
        }
        String pass = encoder.encode(data.senha());
        User user = new User(data.nome(), normalizedEmail, pass, UserRoles.USER);
        return repository.save(user);
    }

    @Override
    public User authUser(LoginUserDTO data) {
        String normalizedEmail = EmailNormalizer.normalized(data.email());
        var emailpass = new UsernamePasswordAuthenticationToken(normalizedEmail, data.senha());
        var auth = authenticationManager.authenticate(emailpass);
        return (User) auth.getPrincipal();
    }

    @Override
    public User findByEmail(String email) {
        return repository.findByEmail(EmailNormalizer.normalized(email));
    }

    @Override
    public Page<ReturnUserDTO> listAllUsers(Pageable pageable) {
        return repository.findAll(pageable)
                .map(user -> new ReturnUserDTO(
                        user.getId(),
                        user.getNome(),
                        user.getEmail(),
                        user.getRole().name(),
                        user.getCreatedAt()
                ));
    }
}
