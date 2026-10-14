package com.jcaa.accounts.infrastructure.entrypoint.rest.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAccountRestRequest(
    @NotBlank(message = "userId must not be blank")
        @Size(max = 36, message = "userId must have at most 36 characters")
        String userId,
    @NotBlank(message = "email must not be blank")
        @Email(message = "email must be a valid email address")
        String email,
    @NotBlank(message = "name must not be blank") String name) {}
