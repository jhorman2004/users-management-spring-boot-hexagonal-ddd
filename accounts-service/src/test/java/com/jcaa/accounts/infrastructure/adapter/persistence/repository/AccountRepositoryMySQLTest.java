package com.jcaa.accounts.infrastructure.adapter.persistence.repository;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jcaa.accounts.domain.enums.AccountStatus;
import com.jcaa.accounts.domain.exception.AccountAlreadyExistsException;
import com.jcaa.accounts.domain.model.AccountModel;
import com.jcaa.accounts.domain.valueobject.AccountUserId;
import com.jcaa.accounts.infrastructure.adapter.persistence.exception.PersistenceException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests para AccountRepositoryMySQL con JDBC simulado (mismo enfoque que UserRepositoryMySQLTest).
 */
@DisplayName("AccountRepositoryMySQL")
@ExtendWith(MockitoExtension.class)
class AccountRepositoryMySQLTest {

  private static final String USER_ID = "u-001";

  @Mock private DataSource dataSource;
  @Mock private Connection connection;
  @Mock private PreparedStatement statement;
  @Mock private ResultSet resultSet;

  private AccountRepositoryMySQL repository;

  @BeforeEach
  void setUp() throws SQLException {
    repository = new AccountRepositoryMySQL(dataSource);
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenReturn(statement);
  }

  @Test
  @DisplayName("save() inserta la cuenta y la devuelve")
  void shouldInsertAccount() throws SQLException {
    // Arrange
    final AccountModel account = AccountModel.openFor(new AccountUserId(USER_ID));

    // Act
    final AccountModel result = repository.save(account);

    // Assert
    assertSame(account, result);
    verify(statement).setString(2, USER_ID);
    verify(statement).executeUpdate();
  }

  @Test
  @DisplayName("save() lanza AccountAlreadyExistsException si la BD detecta user_id duplicado")
  void shouldTranslateDuplicateUserId() throws SQLException {
    // Arrange
    when(statement.executeUpdate())
        .thenThrow(new SQLIntegrityConstraintViolationException("Duplicate entry"));
    final AccountModel account = AccountModel.openFor(new AccountUserId(USER_ID));

    // Act & Assert
    assertThrows(AccountAlreadyExistsException.class, () -> repository.save(account));
  }

  @Test
  @DisplayName("save() lanza PersistenceException ante otros errores SQL")
  void shouldWrapOtherSqlErrors() throws SQLException {
    // Arrange
    when(statement.executeUpdate()).thenThrow(new SQLException("connection lost"));
    final AccountModel account = AccountModel.openFor(new AccountUserId(USER_ID));

    // Act & Assert
    assertThrows(PersistenceException.class, () -> repository.save(account));
  }

  @Test
  @DisplayName("getByUserId() devuelve la cuenta encontrada")
  void shouldReturnAccountWhenFound() throws SQLException {
    // Arrange
    final Instant createdAt = Instant.parse("2026-10-13T10:15:30.123Z");
    when(statement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);
    when(resultSet.getString("id")).thenReturn("acc-1");
    when(resultSet.getString("user_id")).thenReturn(USER_ID);
    when(resultSet.getString("status")).thenReturn("ACTIVE");
    when(resultSet.getTimestamp("created_at")).thenReturn(Timestamp.from(createdAt));

    // Act
    final Optional<AccountModel> result = repository.getByUserId(new AccountUserId(USER_ID));

    // Assert
    assertTrue(result.isPresent());
    final AccountModel account = result.get();
    assertAll(
        () -> assertEquals("acc-1", account.getId().value()),
        () -> assertEquals(AccountStatus.ACTIVE, account.getStatus()),
        () -> assertEquals(createdAt, account.getCreatedAt()));
  }

  @Test
  @DisplayName("getByUserId() devuelve vacío cuando no hay cuenta")
  void shouldReturnEmptyWhenNotFound() throws SQLException {
    // Arrange
    when(statement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(false);

    // Act & Assert
    assertTrue(repository.getByUserId(new AccountUserId(USER_ID)).isEmpty());
  }
}
