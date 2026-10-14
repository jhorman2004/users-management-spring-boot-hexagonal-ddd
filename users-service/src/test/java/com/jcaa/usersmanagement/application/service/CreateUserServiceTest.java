package com.jcaa.usersmanagement.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.jcaa.usersmanagement.application.port.out.CreateAccountPort;
import com.jcaa.usersmanagement.application.port.out.DeleteUserPort;
import com.jcaa.usersmanagement.application.port.out.GetUserByEmailPort;
import com.jcaa.usersmanagement.application.port.out.SaveUserPort;
import com.jcaa.usersmanagement.application.port.out.UserNotificationPort;
import com.jcaa.usersmanagement.application.service.dto.command.CreateUserCommand;
import com.jcaa.usersmanagement.domain.enums.UserRole;
import com.jcaa.usersmanagement.domain.enums.UserStatus;
import com.jcaa.usersmanagement.domain.exception.AccountProvisioningException;
import com.jcaa.usersmanagement.domain.exception.UserAlreadyExistsException;
import com.jcaa.usersmanagement.domain.model.UserModel;
import com.jcaa.usersmanagement.domain.valueobject.UserEmail;
import com.jcaa.usersmanagement.domain.valueobject.UserId;
import com.jcaa.usersmanagement.domain.valueobject.UserName;
import com.jcaa.usersmanagement.domain.valueobject.UserPassword;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests para CreateUserService.
 *
 * <p>Cubre: validación del command, email duplicado, flujo feliz en el orden del diagrama (guardar
 * → crear cuenta → notificar) y compensación cuando falla la creación de la cuenta.
 */
@DisplayName("CreateUserService")
@ExtendWith(MockitoExtension.class)
class CreateUserServiceTest {

  @Mock private SaveUserPort saveUserPort;
  @Mock private GetUserByEmailPort getUserByEmailPort;
  @Mock private DeleteUserPort deleteUserPort;
  @Mock private CreateAccountPort createAccountPort;
  @Mock private UserNotificationPort userNotificationPort;

  private CreateUserService service;

  @BeforeEach
  void setUp() {
    try (final ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
      service =
          new CreateUserService(
              saveUserPort,
              getUserByEmailPort,
              deleteUserPort,
              createAccountPort,
              userNotificationPort,
              validatorFactory.getValidator());
    }
  }

  // ── flujo feliz

  @Test
  @DisplayName("execute() guarda el usuario, crea su cuenta y luego publica la notificación")
  void shouldSaveUserCreateAccountAndNotify() {
    // Arrange
    final CreateUserCommand command =
        new CreateUserCommand("u-01", "John Arrieta", "john@example.com", "Pass1234", "ADMIN");
    final UserModel savedUser = savedUser();

    when(getUserByEmailPort.getByEmail(any())).thenReturn(Optional.empty());
    when(saveUserPort.save(any())).thenReturn(savedUser);

    // Act
    final UserModel result = service.execute(command);

    // Assert
    assertAll(
        "flujo feliz de CreateUserService",
        () -> assertNotNull(result, "resultado no debe ser null"),
        () -> assertEquals("u-01", result.getId().value(), "id del usuario guardado"));

    final InOrder order = inOrder(saveUserPort, createAccountPort, userNotificationPort);
    order.verify(saveUserPort).save(any(UserModel.class));
    order.verify(createAccountPort).createFor(savedUser);
    order.verify(userNotificationPort).notifyUserCreated(savedUser);
    verifyNoInteractions(deleteUserPort);
  }

  // ── compensación

  @Test
  @DisplayName("execute() elimina el usuario y no notifica cuando no se pudo crear la cuenta")
  void shouldCompensateWhenAccountCreationFails() {
    // Arrange
    final CreateUserCommand command =
        new CreateUserCommand("u-01", "John Arrieta", "john@example.com", "Pass1234", "ADMIN");
    final UserModel savedUser = savedUser();
    final AccountProvisioningException failure =
        AccountProvisioningException.becauseAccountServiceFailed("u-01", "HTTP 503");

    when(getUserByEmailPort.getByEmail(any())).thenReturn(Optional.empty());
    when(saveUserPort.save(any())).thenReturn(savedUser);
    doThrow(failure).when(createAccountPort).createFor(savedUser);

    // Act
    final AccountProvisioningException thrown =
        assertThrows(AccountProvisioningException.class, () -> service.execute(command));

    // Assert
    assertSame(failure, thrown, "debe propagar el error original");
    verify(deleteUserPort).delete(savedUser.getId());
    verifyNoInteractions(userNotificationPort);
  }

  // ── email duplicado

  @Test
  @DisplayName("execute() lanza UserAlreadyExistsException cuando el email ya está registrado")
  void shouldThrowWhenEmailAlreadyExists() {
    // Arrange
    final CreateUserCommand command =
        new CreateUserCommand("u-02", "Jane Doe", "jane@example.com", "Pass5678", "MEMBER");

    final UserModel existing =
        new UserModel(
            new UserId("u-99"),
            new UserName("Jane Doe"),
            new UserEmail("jane@example.com"),
            UserPassword.fromHash("$2a$12$hash"),
            UserRole.MEMBER,
            UserStatus.ACTIVE);

    when(getUserByEmailPort.getByEmail(any())).thenReturn(Optional.of(existing));

    // Act & Assert
    assertThrows(UserAlreadyExistsException.class, () -> service.execute(command));
    verify(saveUserPort, never()).save(any());
    verifyNoInteractions(createAccountPort, userNotificationPort);
  }

  // ── validación del command

  @Test
  @DisplayName(
      "execute() lanza ConstraintViolationException cuando el command tiene campos inválidos")
  void shouldThrowWhenCommandIsInvalid() {
    // Arrange — id en blanco y email inválido
    final CreateUserCommand command =
        new CreateUserCommand("", "Jo", "not-an-email", "short", "ADMIN");

    // Act & Assert
    assertThrows(ConstraintViolationException.class, () -> service.execute(command));
    verifyNoInteractions(
        saveUserPort, getUserByEmailPort, createAccountPort, deleteUserPort, userNotificationPort);
  }

  private static UserModel savedUser() {
    return new UserModel(
        new UserId("u-01"),
        new UserName("John Arrieta"),
        new UserEmail("john@example.com"),
        UserPassword.fromHash("$2a$12$hash"),
        UserRole.ADMIN,
        UserStatus.PENDING);
  }
}
