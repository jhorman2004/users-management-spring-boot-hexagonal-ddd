package com.jcaa.usersmanagement.application.service;

import com.jcaa.usersmanagement.application.port.in.CreateUserUseCase;
import com.jcaa.usersmanagement.application.port.out.CreateAccountPort;
import com.jcaa.usersmanagement.application.port.out.DeleteUserPort;
import com.jcaa.usersmanagement.application.port.out.GetUserByEmailPort;
import com.jcaa.usersmanagement.application.port.out.SaveUserPort;
import com.jcaa.usersmanagement.application.port.out.UserNotificationPort;
import com.jcaa.usersmanagement.application.service.dto.command.CreateUserCommand;
import com.jcaa.usersmanagement.application.service.mapper.UserApplicationMapper;
import com.jcaa.usersmanagement.domain.exception.AccountProvisioningException;
import com.jcaa.usersmanagement.domain.exception.UserAlreadyExistsException;
import com.jcaa.usersmanagement.domain.model.UserModel;
import com.jcaa.usersmanagement.domain.valueobject.UserEmail;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;
import jakarta.validation.ConstraintViolation;

/**
 * Registro de usuario (orquesta el diagrama):
 *
 * <ol>
 *   <li>Paso 1: guarda el usuario.
 *   <li>Paso 5: pide a accounts-service crear su cuenta (síncrono). Si falla, se compensa borrando
 *       el usuario, para no dejar un usuario sin cuenta ni mandarle un correo de bienvenida.
 *   <li>Paso 2: publica el aviso de correo (asíncrono, por Kafka).
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreateUserService implements CreateUserUseCase {

  private static final String LOG_COMPENSATING =
      "[CreateUserService] no se pudo crear la cuenta; se elimina el usuario recién creado. userId={}";

  private final SaveUserPort saveUserPort;
  private final GetUserByEmailPort getUserByEmailPort;
  private final DeleteUserPort deleteUserPort;
  private final CreateAccountPort createAccountPort;
  private final UserNotificationPort userNotificationPort;
  private final Validator validator;

  @Override
  public UserModel execute(final CreateUserCommand command) {
    validateCommand(command);

    final UserEmail email = new UserEmail(command.email());
    ensureEmailIsNotTaken(email);

    final UserModel userToSave = UserApplicationMapper.fromCreateCommandToModel(command);
    final UserModel savedUser = saveUserPort.save(userToSave);

    createAccountOrCompensate(savedUser);

    userNotificationPort.notifyUserCreated(savedUser);

    return savedUser;
  }

  private void validateCommand(final CreateUserCommand command) {
    final Set<ConstraintViolation<CreateUserCommand>> violations = validator.validate(command);
    if (!violations.isEmpty()) {
      throw new ConstraintViolationException(violations);
    }
  }

  private void ensureEmailIsNotTaken(final UserEmail email) {
    getUserByEmailPort
        .getByEmail(email)
        .ifPresent(
            ignored -> {
              throw UserAlreadyExistsException.becauseEmailAlreadyExists(email.value());
            });
  }

  private void createAccountOrCompensate(final UserModel savedUser) {
    try {
      createAccountPort.createFor(savedUser);
    } catch (final AccountProvisioningException exception) {
      log.warn(LOG_COMPENSATING, savedUser.getId().value());
      deleteUserPort.delete(savedUser.getId());
      throw exception;
    }
  }
}
