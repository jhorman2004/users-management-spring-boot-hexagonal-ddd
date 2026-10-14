package com.jcaa.accounts.application.port.in;

import com.jcaa.accounts.application.service.dto.command.CreateAccountCommand;
import com.jcaa.accounts.application.service.dto.result.AccountCreationResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface CreateAccountUseCase {
  AccountCreationResult execute(@NotNull @Valid CreateAccountCommand command);
}
