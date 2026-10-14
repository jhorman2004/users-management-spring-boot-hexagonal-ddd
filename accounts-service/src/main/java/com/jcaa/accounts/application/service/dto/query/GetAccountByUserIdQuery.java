package com.jcaa.accounts.application.service.dto.query;

import jakarta.validation.constraints.NotBlank;

public record GetAccountByUserIdQuery(
    @NotBlank(message = "userId must not be blank") String userId) {}
