package com.ufc.apiPenduraAi.dtos.divida;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateDividaDTO(
        @NotBlank(message = "Nome do cliente é obrigatório")
        @JsonAlias("nome")
        @Size(max = 100, message = "nome do cliente até 100 caracteres somente.")
        String cliente,

        @NotNull(message = "Valor é obrigatório")
        @DecimalMin(value = "0.01", message = "O valor deve ser de no mínimo 0.01")
        @Digits(integer = 8, fraction = 2, message = "apenas 8 número antes da vírgula e 2 depois.")
        BigDecimal valor
) {
}
