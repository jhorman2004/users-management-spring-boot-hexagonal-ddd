package com.jcaa.accounts.domain.model;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jcaa.accounts.domain.enums.AccountStatus;
import com.jcaa.accounts.domain.exception.InvalidAccountDataException;
import com.jcaa.accounts.domain.valueobject.AccountUserId;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("AccountModel")
class AccountModelTest {

  @Test
  @DisplayName("openFor() crea una cuenta activa, con id nuevo y fecha en milisegundos")
  void shouldOpenActiveAccount() {
    // Arrange
    final AccountUserId userId = new AccountUserId("u-001");

    // Act
    final AccountModel first = AccountModel.openFor(userId);
    final AccountModel second = AccountModel.openFor(userId);

    // Assert
    final Instant createdAt = first.getCreatedAt();
    assertAll(
        () -> assertEquals(AccountStatus.ACTIVE, first.getStatus()),
        () -> assertEquals(userId, first.getUserId()),
        () -> assertNotEquals(first.getId(), second.getId()),
        () -> assertEquals(createdAt, createdAt.truncatedTo(ChronoUnit.MILLIS)));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  @DisplayName("AccountUserId rechaza valores vacíos")
  void shouldRejectBlankUserId(final String value) {
    // Act & Assert
    assertThrows(InvalidAccountDataException.class, () -> new AccountUserId(value));
  }

  @Test
  @DisplayName("AccountUserId rechaza valores de más de 36 caracteres")
  void shouldRejectTooLongUserId() {
    // Act & Assert
    assertThrows(InvalidAccountDataException.class, () -> new AccountUserId("x".repeat(37)));
  }
}
