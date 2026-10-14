package com.jcaa.events.notification;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Mensaje "nuevo correo por enviar" que viaja por el tópico de notificaciones.
 *
 * <p>{@code eventId} identifica el mensaje de forma única: el notificador lo usa para no enviar dos
 * veces el mismo correo si Kafka lo entrega repetido. {@code data} lleva solo los valores que la
 * plantilla necesita; nunca contraseñas.
 */
public record EmailNotificationEvent(
    String eventId,
    NotificationType type,
    Instant occurredAt,
    String recipientEmail,
    String recipientName,
    Map<String, String> data) {

  public EmailNotificationEvent {
    requireText(eventId, "eventId");
    if (Objects.isNull(type)) {
      throw new IllegalArgumentException("type is required");
    }
    if (Objects.isNull(occurredAt)) {
      throw new IllegalArgumentException("occurredAt is required");
    }
    requireText(recipientEmail, "recipientEmail");
    requireText(recipientName, "recipientName");
    data = Objects.isNull(data) ? Map.of() : Map.copyOf(data);
  }

  /** Crea un evento nuevo con identificador aleatorio y fecha actual. */
  public static EmailNotificationEvent of(
      final NotificationType type,
      final String recipientEmail,
      final String recipientName,
      final Map<String, String> data) {
    return new EmailNotificationEvent(
        UUID.randomUUID().toString(), type, Instant.now(), recipientEmail, recipientName, data);
  }

  private static void requireText(final String value, final String fieldName) {
    if (Objects.isNull(value) || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
  }
}
