package com.ufc.apiPenduraAi.controllers.divida;

import com.ufc.apiPenduraAi.dtos.divida.CreateDividaDTO;
import com.ufc.apiPenduraAi.dtos.divida.ReturnDividasDTO;
import com.ufc.apiPenduraAi.dtos.divida.UpdateDividaDTO;
import com.ufc.apiPenduraAi.services.divida.DividaServices;
import com.ufc.apiPenduraAi.utils.PageableSortValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/divida")
@RequiredArgsConstructor
public class DividaController {

    private static final Set<String> ALLOWEDFIELDS = Set.of("cliente", "valor", "createdAt");

    private final DividaServices services;

    @PostMapping
    public ResponseEntity<String> createDivida(@RequestBody @Valid CreateDividaDTO data) {
        services.addDivida(data);
        return ResponseEntity.status(HttpStatus.CREATED).body("Dívida cadastrada com sucesso!");
    }

    @GetMapping
    public ResponseEntity<Page<ReturnDividasDTO>> pesquisarDivida(
            @RequestParam(name = "cliente", required = false, defaultValue = "") String cliente,
            @PageableDefault(size = 10, sort = "cliente") Pageable pageable
    ) {

        PageableSortValidator.validate(pageable, ALLOWEDFIELDS);

        Page<ReturnDividasDTO> response = services.findDivida(cliente, pageable);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/update")
    public ResponseEntity<String> updateDivida(@PathVariable Long id, @RequestBody @Valid UpdateDividaDTO data) {
        services.updateValor(data, id);
        return ResponseEntity.ok("Valor atualizado com sucesso!");
    }

    @DeleteMapping("/{id}/quitar")
    public ResponseEntity<String> quitarDivida(@PathVariable Long id) {
        services.quitarDivida(id);
        return ResponseEntity.ok("Dívida quitada com sucesso!");
    }
}
