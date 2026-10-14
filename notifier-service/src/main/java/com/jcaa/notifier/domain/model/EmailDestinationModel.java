package com.jcaa.notifier.domain.model;

import java.util.Objects;
import lombok.Value;

/**
 * Correo listo para enviar. {@code referenceId} es el {@code eventId} del mensaje de Kafka: se usa
 * como clave de idempotencia ante Brevo y para identificar el envío en los logs.
 */
@Value
public class EmailDestinationModel {

  String referenceId;
  String destinationEmail;
  String destinationName;
  String subject;
  String body;

  public EmailDestinationModel(
      final String referenceId,
      final String destinationEmail,
      final String destinationName,
      final String subject,
      final String body) {
    this.referenceId = validateNotBlank(referenceId, "La referencia del envío es requerida.");
    this.destinationEmail =
        validateNotBlank(destinationEmail, "El email del destinatario es requerido.");
    this.destinationName =
        validateNotBlank(destinationName, "El nombre del destinatario es requerido.");
    this.subject = validateNotBlank(subject, "El asunto es requerido.");
    this.body = validateNotBlank(body, "El cuerpo del mensaje es requerido.");
  }

  private static String validateNotBlank(final String value, final String errorMessage) {
    Objects.requireNonNull(value, errorMessage);
    if (value.trim().isEmpty()) {
      throw new IllegalArgumentException(errorMessage);
    }
    return value;
  }
}
