package com.jcaa.accounts.application.port.in;

import com.jcaa.accounts.application.service.dto.query.GetAccountByUserIdQuery;
import com.jcaa.accounts.domain.model.AccountModel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface GetAccountByUserIdUseCase {
  AccountModel execute(@NotNull @Valid GetAccountByUserIdQuery query);
}
