package com.jcaa.accounts.domain.exception;

/**
 * Otra petición creó la cuenta de ese usuario al mismo tiempo (lo detecta la restricción UNIQUE de
 * {@code user_id}). El caso de uso la trata como "ya existía".
 */
public final class AccountAlreadyExistsException extends DomainException {

  private static final String MESSAGE_BY_USER = "El usuario '%s' ya tiene una cuenta.";

  private AccountAlreadyExistsException(final String message) {
    super(message);
  }

  public static AccountAlreadyExistsException becauseUserAlreadyHasAccount(final String userId) {
    return new AccountAlreadyExistsException(String.format(MESSAGE_BY_USER, userId));
  }
}
