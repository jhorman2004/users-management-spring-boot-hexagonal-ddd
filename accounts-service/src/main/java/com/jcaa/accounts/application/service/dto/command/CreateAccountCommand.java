package com.jcaa.accounts.application.service.dto.command;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** El correo y el nombre solo se usan para el aviso por correo; la cuenta no los guarda. */
public record CreateAccountCommand(
    @NotBlank(message = "userId must not be blank")
        @Size(max = 36, message = "userId must have at most 36 characters")
        String userId,
    @NotBlank(message = "email must not be blank")
        @Email(message = "email must be a valid email address")
        String email,
    @NotBlank(message = "name must not be blank") String name) {}
