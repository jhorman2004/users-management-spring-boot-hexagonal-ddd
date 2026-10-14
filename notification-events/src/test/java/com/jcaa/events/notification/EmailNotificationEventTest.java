package com.jcaa.events.notification;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("EmailNotificationEvent")
class EmailNotificationEventTest {

  private static final String EMAIL = "john@example.com";
  private static final String NAME = "John Arrieta";

  @Test
  @DisplayName("of() genera un eventId único y la fecha actual")
  void shouldGenerateIdAndDateWhenUsingFactory() {
    // Arrange
    final Instant before = Instant.now();

    // Act
    final EmailNotificationEvent first =
        EmailNotificationEvent.of(NotificationType.USER_CREATED, EMAIL, NAME, Map.of());
    final EmailNotificationEvent second =
        EmailNotificationEvent.of(NotificationType.USER_CREATED, EMAIL, NAME, Map.of());

    // Assert
    assertAll(
        () -> assertNotNull(first.eventId()),
        () -> assertNotEquals(first.eventId(), second.eventId()),
        () -> assertFalse(first.occurredAt().isBefore(before)));
  }

  @Test
  @DisplayName("copia data para que no se pueda modificar desde afuera")
  void shouldCopyDataDefensively() {
    // Arrange
    final Map<String, String> data = new HashMap<>(Map.of("role", "ADMIN"));
    final EmailNotificationEvent event =
        EmailNotificationEvent.of(NotificationType.USER_CREATED, EMAIL, NAME, data);

    // Act
    data.put("role", "MEMBER");

    // Assert
    assertAll(
        () -> assertEquals("ADMIN", event.data().get("role")),
        () ->
            assertThrows(
                UnsupportedOperationException.class, () -> event.data().put("x", "y")));
  }

  @Test
  @DisplayName("acepta data nula y la convierte en un mapa vacío")
  void shouldAcceptNullData() {
    // Act
    final EmailNotificationEvent event =
        EmailNotificationEvent.of(NotificationType.ACCOUNT_CREATED, EMAIL, NAME, null);

    // Assert
    assertTrue(event.data().isEmpty());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  @DisplayName("rechaza un correo de destino vacío")
  void shouldRejectBlankRecipientEmail(final String recipientEmail) {
    // Act & Assert
    assertThrows(
        IllegalArgumentException.class,
        () -> EmailNotificationEvent.of(NotificationType.USER_CREATED, recipientEmail, NAME, null));
  }

  @Test
  @DisplayName("rechaza un evento sin tipo")
  void shouldRejectMissingType() {
    // Act & Assert
    assertThrows(
        IllegalArgumentException.class,
        () -> new EmailNotificationEvent("id-1", null, Instant.now(), EMAIL, NAME, Map.of()));
  }

  @Test
  @DisplayName("acepta un evento completo construido a mano")
  void shouldAcceptCompleteEvent() {
    // Act & Assert
    assertDoesNotThrow(
        () ->
            new EmailNotificationEvent(
                "id-1", NotificationType.USER_UPDATED, Instant.now(), EMAIL, NAME, Map.of()));
  }
}
