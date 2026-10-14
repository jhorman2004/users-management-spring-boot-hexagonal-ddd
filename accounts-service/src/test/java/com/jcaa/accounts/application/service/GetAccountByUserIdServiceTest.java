package com.jcaa.accounts.application.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.jcaa.accounts.application.port.out.GetAccountByUserIdPort;
import com.jcaa.accounts.application.service.dto.query.GetAccountByUserIdQuery;
import com.jcaa.accounts.domain.exception.AccountNotFoundException;
import com.jcaa.accounts.domain.model.AccountModel;
import com.jcaa.accounts.domain.valueobject.AccountUserId;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("GetAccountByUserIdService")
@ExtendWith(MockitoExtension.class)
class GetAccountByUserIdServiceTest {

  @Mock private GetAccountByUserIdPort getAccountByUserIdPort;

  private GetAccountByUserIdService service;

  @BeforeEach
  void setUp() {
    try (final ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
      service = new GetAccountByUserIdService(getAccountByUserIdPort, validatorFactory.getValidator());
    }
  }

  @Test
  @DisplayName("execute() devuelve la cuenta del usuario")
  void shouldReturnAccount() {
    // Arrange
    final AccountModel account = AccountModel.openFor(new AccountUserId("u-001"));
    when(getAccountByUserIdPort.getByUserId(any())).thenReturn(Optional.of(account));

    // Act
    final AccountModel result = service.execute(new GetAccountByUserIdQuery("u-001"));

    // Assert
    assertSame(account, result);
  }

  @Test
  @DisplayName("execute() lanza AccountNotFoundException cuando el usuario no tiene cuenta")
  void shouldThrowWhenAccountDoesNotExist() {
    // Arrange
    when(getAccountByUserIdPort.getByUserId(any())).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(
        AccountNotFoundException.class,
        () -> service.execute(new GetAccountByUserIdQuery("u-404")));
  }
}
