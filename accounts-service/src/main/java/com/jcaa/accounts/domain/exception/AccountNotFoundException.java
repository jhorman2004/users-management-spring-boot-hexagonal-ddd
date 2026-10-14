package com.jcaa.accounts.domain.exception;

public final class AccountNotFoundException extends DomainException {

  private static final String MESSAGE_BY_USER = "El usuario '%s' no tiene una cuenta.";

  private AccountNotFoundException(final String message) {
    super(message);
  }

  public static AccountNotFoundException becauseUserHasNoAccount(final String userId) {
    return new AccountNotFoundException(String.format(MESSAGE_BY_USER, userId));
  }
}
