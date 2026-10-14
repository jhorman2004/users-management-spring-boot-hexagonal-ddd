package com.jcaa.usersmanagement.domain.exception;

/** No se pudo crear la cuenta del usuario en accounts-service (paso 5 del diagrama). */
public final class AccountProvisioningException extends DomainException {

  private static final String MESSAGE_SERVICE_FAILED =
      "No se pudo crear la cuenta del usuario '%s' (%s). Intenta de nuevo en unos minutos.";

  private AccountProvisioningException(final String message) {
    super(message);
  }

  public static AccountProvisioningException becauseAccountServiceFailed(
      final String userId, final String detail) {
    return new AccountProvisioningException(String.format(MESSAGE_SERVICE_FAILED, userId, detail));
  }
}
