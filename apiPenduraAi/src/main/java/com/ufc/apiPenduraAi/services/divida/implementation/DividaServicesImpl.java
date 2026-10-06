package com.ufc.apiPenduraAi.services.divida.implementation;

import com.ufc.apiPenduraAi.domain.divida.Divida;
import com.ufc.apiPenduraAi.domain.user.User;
import com.ufc.apiPenduraAi.dtos.divida.CreateDividaDTO;
import com.ufc.apiPenduraAi.dtos.divida.ReturnDividasDTO;
import com.ufc.apiPenduraAi.dtos.divida.UpdateDividaDTO;
import com.ufc.apiPenduraAi.exceptions.divida.NotFoundDivida;
import com.ufc.apiPenduraAi.repositories.divida.DividaRepository;

import com.ufc.apiPenduraAi.services.divida.DividaServices;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DividaServicesImpl implements DividaServices {

    private final DividaRepository repository;

    private User getAuthenticatedUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();

        if(auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof User user)){
            throw new AuthenticationCredentialsNotFoundException("Usuário não autenticado");
        }

        return user;
    }

    @Override
    public void addDivida(CreateDividaDTO data) {
        User user = getAuthenticatedUser();
        Divida novaDivida = new Divida(data.cliente(), data.valor(), user);
        repository.save(novaDivida);
    }

    @Override
    public Page<ReturnDividasDTO> findDivida(String cliente, Pageable pageable) {
        User user = getAuthenticatedUser();
        String searchString = cliente != null ? cliente : "";
        Page<Divida> devedores = repository.findAllByUserAndClienteContainingIgnoreCase(user, searchString, pageable);
        return devedores.map(d -> new ReturnDividasDTO(d.getId(), d.getCliente(), d.getValor(), d.getCreatedAt()));
    }

    @Override
    public void updateValor(UpdateDividaDTO data, Long id) {
        User user = getAuthenticatedUser();
        Divida divida = repository.findByIdAndUser(id, user)
                .orElseThrow(() -> new NotFoundDivida("Dívida não encontrada!"));

        divida.setValor(data.novoValor());
        repository.save(divida);
    }

    @Override
    public void quitarDivida(Long id) {
        User user = getAuthenticatedUser();
        Divida divida = repository.findByIdAndUser(id, user)
                .orElseThrow(() -> new NotFoundDivida("Dívida não encontrada!"));

        repository.delete(divida);
    }
}
