package com.ufc.apiPenduraAi.dtos;

import com.ufc.apiPenduraAi.dtos.divida.CreateDividaDTO;
import com.ufc.apiPenduraAi.dtos.divida.UpdateDividaDTO;
import com.ufc.apiPenduraAi.dtos.user.CreateUserDTO;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DtoValidationTest {

    private ValidatorFactory validatorFactory;
    private Validator validator;

    @BeforeEach
    void setUp(){
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterEach
    void tearDown(){
        validatorFactory.close();
    }

    @Test
    void createUserRejectsFieldsLargerThanDatabaseColumns() {
        CreateUserDTO dto = new CreateUserDTO(
                "A".repeat(51),
                "a".repeat(246) + "@mail.com",
                "123456789"
        );

        assertAll(
                () -> assertTrue(
                        hasViolation(dto, "nome", Size.class)
                ),
                () -> assertTrue(
                        hasViolation(dto, "email", Size.class)
                )
        );
    }

    @Test
    void createDeptsRejectsClientAndValueOutsideDatabaseLimits(){
        CreateDividaDTO dto = new CreateDividaDTO(
                "A".repeat(101),
                new BigDecimal("100000000.999")
        );

        assertAll(
                () -> assertTrue(
                        hasViolation(dto, "cliente", Size.class)
                ),
                () -> assertTrue(
                        hasViolation(dto, "valor", Digits.class)
                )
        );
    }

    @Test
    void updateDeptRejectsValueOutsideDatebaseLimits(){
        UpdateDividaDTO dto = new UpdateDividaDTO(
                new BigDecimal("10.999")
        );

        assertTrue(hasViolation(dto, "novoValor", Digits.class));
    }

    private boolean hasViolation(Object dto, String field, Class<? extends Annotation> constraint){
        return validator.validate(dto)
                .stream()
                .anyMatch(violation ->
                        violation.getPropertyPath().toString().equals(field) && violation.getConstraintDescriptor().getAnnotation().annotationType().equals(constraint));

    }
}

