package com.jcaa.accounts.domain.exception;

public final class InvalidAccountDataException extends DomainException {

  private static final String MESSAGE_BLANK = "El campo '%s' de la cuenta es requerido.";
  private static final String MESSAGE_TOO_LONG =
      "El campo '%s' de la cuenta no puede superar %d caracteres.";

  private InvalidAccountDataException(final String message) {
    super(message);
  }

  public static InvalidAccountDataException becauseValueIsBlank(final String fieldName) {
    return new InvalidAccountDataException(String.format(MESSAGE_BLANK, fieldName));
  }

  public static InvalidAccountDataException becauseValueIsTooLong(
      final String fieldName, final int maxLength) {
    return new InvalidAccountDataException(String.format(MESSAGE_TOO_LONG, fieldName, maxLength));
  }
}
