package com.jcaa.notifier.domain.exception;

/**
 * Fallo al preparar o enviar un correo. Los mensajes identifican el envío por la referencia del
 * evento, nunca por el correo del destinatario, para no dejar datos personales en los logs.
 */
public final class EmailSenderException extends DomainException {

  private static final String DEFAULT_MESSAGE = "La notificación por correo no pudo ser enviada.";
  private static final String API_MESSAGE_WITH_DETAIL =
      "No se pudo enviar el correo del evento '%s' mediante la API de Brevo: %s";
  private static final String TEMPLATE_NOT_FOUND_MESSAGE = "No existe la plantilla de correo '%s'.";

  public EmailSenderException(final String message) {
    super(message);
  }

  public EmailSenderException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public static EmailSenderException becauseSendFailed(final Throwable cause) {
    return new EmailSenderException(DEFAULT_MESSAGE, cause);
  }

  public static EmailSenderException becauseApiFailed(
      final String referenceId, final String detail) {
    return new EmailSenderException(String.format(API_MESSAGE_WITH_DETAIL, referenceId, detail));
  }

  public static EmailSenderException becauseTemplateWasNotFound(final String templateName) {
    return new EmailSenderException(String.format(TEMPLATE_NOT_FOUND_MESSAGE, templateName));
  }
}
