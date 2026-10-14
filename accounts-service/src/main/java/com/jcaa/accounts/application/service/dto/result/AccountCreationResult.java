package com.jcaa.accounts.application.service.dto.result;

import com.jcaa.accounts.domain.model.AccountModel;

/** {@code created} es false cuando el usuario ya tenía cuenta y se devolvió la existente. */
public record AccountCreationResult(AccountModel account, boolean created) {}
