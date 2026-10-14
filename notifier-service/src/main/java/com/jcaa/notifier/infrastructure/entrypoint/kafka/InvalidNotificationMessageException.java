package com.jcaa.notifier.infrastructure.entrypoint.kafka;

/** El mensaje recibido no es un {@code EmailNotificationEvent} válido. Va directo a la DLT. */
public final class InvalidNotificationMessageException extends RuntimeException {

  private static final String MESSAGE_INVALID_PAYLOAD =
      "El mensaje de notificación no tiene un formato válido.";

  private InvalidNotificationMessageException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public static InvalidNotificationMessageException becausePayloadIsNotValid(
      final Throwable cause) {
    return new InvalidNotificationMessageException(MESSAGE_INVALID_PAYLOAD, cause);
  }
}
