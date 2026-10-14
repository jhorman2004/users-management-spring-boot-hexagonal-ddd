package com.jcaa.accounts.application.service;

import com.jcaa.accounts.application.port.in.GetAccountByUserIdUseCase;
import com.jcaa.accounts.application.port.out.GetAccountByUserIdPort;
import com.jcaa.accounts.application.service.dto.query.GetAccountByUserIdQuery;
import com.jcaa.accounts.domain.exception.AccountNotFoundException;
import com.jcaa.accounts.domain.model.AccountModel;
import com.jcaa.accounts.domain.valueobject.AccountUserId;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetAccountByUserIdService implements GetAccountByUserIdUseCase {

  private final GetAccountByUserIdPort getAccountByUserIdPort;
  private final Validator validator;

  @Override
  public AccountModel execute(final GetAccountByUserIdQuery query) {
    validateQuery(query);
    final AccountUserId userId = new AccountUserId(query.userId());
    return getAccountByUserIdPort
        .getByUserId(userId)
        .orElseThrow(() -> AccountNotFoundException.becauseUserHasNoAccount(userId.value()));
  }

  private void validateQuery(final GetAccountByUserIdQuery query) {
    final Set<ConstraintViolation<GetAccountByUserIdQuery>> violations = validator.validate(query);
    if (!violations.isEmpty()) {
      throw new ConstraintViolationException(violations);
    }
  }
}
