package com.jcaa.accounts.application.service;

import com.jcaa.accounts.application.port.in.CreateAccountUseCase;
import com.jcaa.accounts.application.port.out.AccountNotificationPort;
import com.jcaa.accounts.application.port.out.GetAccountByUserIdPort;
import com.jcaa.accounts.application.port.out.SaveAccountPort;
import com.jcaa.accounts.application.service.dto.command.CreateAccountCommand;
import com.jcaa.accounts.application.service.dto.result.AccountCreationResult;
import com.jcaa.accounts.domain.exception.AccountAlreadyExistsException;
import com.jcaa.accounts.domain.exception.AccountNotFoundException;
import com.jcaa.accounts.domain.model.AccountModel;
import com.jcaa.accounts.domain.valueobject.AccountUserId;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Pasos 6 y 7 del diagrama: guarda la cuenta y publica el aviso de correo.
 *
 * <p>Es idempotente: users-service reintenta si no recibe respuesta, así que si el usuario ya tiene
 * cuenta se devuelve la existente, sin crear otra ni repetir el correo.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreateAccountService implements CreateAccountUseCase {

  private static final String LOG_CREATED =
      "[CreateAccountService] cuenta creada. accountId={} userId={}";
  private static final String LOG_ALREADY_EXISTS =
      "[CreateAccountService] el usuario ya tenía cuenta, se devuelve la existente. userId={}";

  private final SaveAccountPort saveAccountPort;
  private final GetAccountByUserIdPort getAccountByUserIdPort;
  private final AccountNotificationPort accountNotificationPort;
  private final Validator validator;

  @Override
  public AccountCreationResult execute(final CreateAccountCommand command) {
    validateCommand(command);
    final AccountUserId userId = new AccountUserId(command.userId());

    final Optional<AccountModel> existing = getAccountByUserIdPort.getByUserId(userId);
    if (existing.isPresent()) {
      log.info(LOG_ALREADY_EXISTS, userId.value());
      return new AccountCreationResult(existing.get(), false);
    }

    final AccountModel saved;
    try {
      saved = saveAccountPort.save(AccountModel.openFor(userId));
    } catch (final AccountAlreadyExistsException concurrentCreation) {
      log.info(LOG_ALREADY_EXISTS, userId.value());
      return new AccountCreationResult(findExistingOrFail(userId), false);
    }

    accountNotificationPort.notifyAccountCreated(saved, command.email(), command.name());
    log.info(LOG_CREATED, saved.getId().value(), userId.value());
    return new AccountCreationResult(saved, true);
  }

  private void validateCommand(final CreateAccountCommand command) {
    final Set<ConstraintViolation<CreateAccountCommand>> violations = validator.validate(command);
    if (!violations.isEmpty()) {
      throw new ConstraintViolationException(violations);
    }
  }

  private AccountModel findExistingOrFail(final AccountUserId userId) {
    return getAccountByUserIdPort
        .getByUserId(userId)
        .orElseThrow(() -> AccountNotFoundException.becauseUserHasNoAccount(userId.value()));
  }
}
