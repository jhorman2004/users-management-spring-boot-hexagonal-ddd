package com.jcaa.accounts.application.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.jcaa.accounts.application.port.out.AccountNotificationPort;
import com.jcaa.accounts.application.port.out.GetAccountByUserIdPort;
import com.jcaa.accounts.application.port.out.SaveAccountPort;
import com.jcaa.accounts.application.service.dto.command.CreateAccountCommand;
import com.jcaa.accounts.application.service.dto.result.AccountCreationResult;
import com.jcaa.accounts.domain.enums.AccountStatus;
import com.jcaa.accounts.domain.exception.AccountAlreadyExistsException;
import com.jcaa.accounts.domain.model.AccountModel;
import com.jcaa.accounts.domain.valueobject.AccountUserId;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests para CreateAccountService.
 *
 * <p>Cubre: cuenta nueva (guarda + avisa), usuario que ya tenía cuenta (idempotencia), creación
 * concurrente detectada por la BD y validación del command.
 */
@DisplayName("CreateAccountService")
@ExtendWith(MockitoExtension.class)
class CreateAccountServiceTest {

  private static final String USER_ID = "u-001";
  private static final String EMAIL = "john@example.com";
  private static final String NAME = "John Arrieta";

  @Mock private SaveAccountPort saveAccountPort;
  @Mock private GetAccountByUserIdPort getAccountByUserIdPort;
  @Mock private AccountNotificationPort accountNotificationPort;

  private CreateAccountService service;

  @BeforeEach
  void setUp() {
    try (final ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
      service =
          new CreateAccountService(
              saveAccountPort,
              getAccountByUserIdPort,
              accountNotificationPort,
              validatorFactory.getValidator());
    }
  }

  @Test
  @DisplayName("execute() crea una cuenta activa y avisa cuando el usuario no tenía cuenta")
  void shouldCreateAccountAndNotify() {
    // Arrange
    when(getAccountByUserIdPort.getByUserId(any())).thenReturn(Optional.empty());
    when(saveAccountPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    final AccountCreationResult result = service.execute(command());

    // Assert
    assertAll(
        () -> assertTrue(result.created(), "debe indicar que se creó"),
        () -> assertEquals(USER_ID, result.account().getUserId().value()),
        () -> assertEquals(AccountStatus.ACTIVE, result.account().getStatus()));
    verify(accountNotificationPort).notifyAccountCreated(result.account(), EMAIL, NAME);
  }

  @Test
  @DisplayName("execute() devuelve la cuenta existente sin guardar ni avisar otra vez")
  void shouldReturnExistingAccountWithoutNotifying() {
    // Arrange
    final AccountModel existing = AccountModel.openFor(new AccountUserId(USER_ID));
    when(getAccountByUserIdPort.getByUserId(any())).thenReturn(Optional.of(existing));

    // Act
    final AccountCreationResult result = service.execute(command());

    // Assert
    assertAll(
        () -> assertFalse(result.created()), () -> assertSame(existing, result.account()));
    verify(saveAccountPort, never()).save(any());
    verifyNoInteractions(accountNotificationPort);
  }

  @Test
  @DisplayName("execute() devuelve la cuenta creada por una petición concurrente")
  void shouldReturnAccountCreatedConcurrently() {
    // Arrange
    final AccountModel concurrent = AccountModel.openFor(new AccountUserId(USER_ID));
    when(getAccountByUserIdPort.getByUserId(any()))
        .thenReturn(Optional.empty())
        .thenReturn(Optional.of(concurrent));
    when(saveAccountPort.save(any()))
        .thenThrow(AccountAlreadyExistsException.becauseUserAlreadyHasAccount(USER_ID));

    // Act
    final AccountCreationResult result = service.execute(command());

    // Assert
    assertAll(
        () -> assertFalse(result.created()), () -> assertSame(concurrent, result.account()));
    verifyNoInteractions(accountNotificationPort);
  }

  @Test
  @DisplayName("execute() lanza ConstraintViolationException cuando el command es inválido")
  void shouldThrowWhenCommandIsInvalid() {
    // Arrange
    final CreateAccountCommand invalid = new CreateAccountCommand("", "no-es-email", "");

    // Act & Assert
    assertThrows(ConstraintViolationException.class, () -> service.execute(invalid));
    verifyNoInteractions(getAccountByUserIdPort, saveAccountPort, accountNotificationPort);
  }

  private static CreateAccountCommand command() {
    return new CreateAccountCommand(USER_ID, EMAIL, NAME);
  }
}
