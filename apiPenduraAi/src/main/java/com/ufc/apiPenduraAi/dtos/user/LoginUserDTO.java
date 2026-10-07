package com.ufc.apiPenduraAi.dtos.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginUserDTO(
        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email inválido")
        @Size(max = 100, message = "Email com até 100 caracteres permitido")
        String email,

        @NotBlank(message = "Senha é obrigatória")
        String senha
) {
}
