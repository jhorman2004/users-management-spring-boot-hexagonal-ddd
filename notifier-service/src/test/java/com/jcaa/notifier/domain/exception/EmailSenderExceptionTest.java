package com.jcaa.notifier.domain.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests para EmailSenderException.
 *
 * <p>De paso cubre el constructor de dos argumentos de {@code DomainException} (abstracta),
 * alcanzable sólo a través de {@code becauseSendFailed(cause)}.
 */
@DisplayName("EmailSenderException")
class EmailSenderExceptionTest {

  // ── becauseApiFailed()

  @Test
  @DisplayName("becauseApiFailed() incluye la referencia del evento y el detalle de la API")
  void shouldFormatMessageWithReferenceAndApiError() {
    // Arrange
    final String referenceId = "event-123";
    final String apiError = "HTTP 503";

    // Act
    final String message =
        EmailSenderException.becauseApiFailed(referenceId, apiError).getMessage();

    // Assert
    assertAll(
        "becauseApiFailed",
        () -> assertTrue(message.contains(referenceId), "el mensaje debe contener la referencia"),
        () -> assertTrue(message.contains(apiError), "el mensaje debe contener el error de la API"));
  }

  // ── becauseTemplateWasNotFound()

  @Test
  @DisplayName("becauseTemplateWasNotFound() incluye el nombre de la plantilla")
  void shouldFormatMessageWithTemplateName() {
    // Act
    final String message =
        EmailSenderException.becauseTemplateWasNotFound("missing.html").getMessage();

    // Assert
    assertTrue(message.contains("missing.html"), "el mensaje debe contener la plantilla");
  }

  // ── becauseSendFailed()

  @Test
  @DisplayName("becauseSendFailed() debe encapsular la causa y producir un mensaje no vacío")
  void shouldWrapCauseAndProduceNonBlankMessage() {
    // Arrange
    final Throwable cause = new RuntimeException("IO error");

    // Act
    final EmailSenderException exception = EmailSenderException.becauseSendFailed(cause);

    // Assert
    assertAll(
        "becauseSendFailed",
        () -> assertSame(cause, exception.getCause(), "debe encapsular la causa original"),
        () ->
            assertFalse(
                exception.getMessage().isBlank(), "el mensaje por defecto no debe estar vacío"));
  }
}
