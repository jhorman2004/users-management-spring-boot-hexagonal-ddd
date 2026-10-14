package com.jcaa.notifier.application.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.jcaa.events.notification.NotificationType;
import com.jcaa.notifier.application.port.out.EmailSenderPort;
import com.jcaa.notifier.application.port.out.EmailTemplatePort;
import com.jcaa.notifier.application.port.out.ProcessedNotificationPort;
import com.jcaa.notifier.application.service.dto.command.SendEmailNotificationCommand;
import com.jcaa.notifier.domain.exception.EmailSenderException;
import com.jcaa.notifier.domain.model.EmailDestinationModel;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests para SendEmailNotificationService.
 *
 * <p>Cubre: plantilla y asunto por tipo, valores enviados a la plantilla, idempotencia (evento
 * repetido) y que un fallo de envío no marca el evento como procesado.
 */
@DisplayName("SendEmailNotificationService")
@ExtendWith(MockitoExtension.class)
class SendEmailNotificationServiceTest {

  private static final String EVENT_ID = "event-1";
  private static final String EMAIL = "john@example.com";
  private static final String NAME = "John Arrieta";
  private static final String BODY = "<html>body</html>";

  @Mock private EmailTemplatePort emailTemplatePort;
  @Mock private EmailSenderPort emailSenderPort;
  @Mock private ProcessedNotificationPort processedNotificationPort;

  @Captor private ArgumentCaptor<EmailDestinationModel> destinationCaptor;
  @Captor private ArgumentCaptor<Map<String, String>> valuesCaptor;

  private SendEmailNotificationService service;

  @BeforeEach
  void setUp() {
    try (final ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
      service =
          new SendEmailNotificationService(
              emailTemplatePort,
              emailSenderPort,
              processedNotificationPort,
              validatorFactory.getValidator());
    }
  }

  // ── plantilla y asunto según el tipo

  @ParameterizedTest
  @CsvSource({
    "USER_CREATED, user-created.html, creada",
    "USER_UPDATED, user-updated.html, actualizada",
    "ACCOUNT_CREATED, account-created.html, acceso"
  })
  @DisplayName("execute() usa la plantilla y el asunto que corresponden al tipo")
  void shouldUseTemplateAndSubjectForType(
      final NotificationType type, final String template, final String subjectWord) {
    // Arrange
    when(processedNotificationPort.isAlreadyProcessed(EVENT_ID)).thenReturn(false);
    when(emailTemplatePort.render(eq(template), anyMap())).thenReturn(BODY);

    // Act
    service.execute(command(type, Map.of()));

    // Assert
    verify(emailSenderPort).send(destinationCaptor.capture());
    final EmailDestinationModel sent = destinationCaptor.getValue();
    assertAll(
        () -> assertEquals(EVENT_ID, sent.getReferenceId(), "referencia = eventId"),
        () -> assertEquals(EMAIL, sent.getDestinationEmail(), "destinatario"),
        () -> assertEquals(BODY, sent.getBody(), "cuerpo renderizado"),
        () -> assertTrue(sent.getSubject().contains(subjectWord), "asunto"));
    verify(processedNotificationPort).markAsProcessed(EVENT_ID);
  }

  @Test
  @DisplayName("execute() pasa a la plantilla los datos del evento más nombre y correo")
  void shouldPassEventDataAndRecipientToTemplate() {
    // Arrange
    when(processedNotificationPort.isAlreadyProcessed(EVENT_ID)).thenReturn(false);
    when(emailTemplatePort.render(any(), anyMap())).thenReturn(BODY);

    // Act
    service.execute(command(NotificationType.USER_CREATED, Map.of("role", "ADMIN")));

    // Assert
    verify(emailTemplatePort).render(eq("user-created.html"), valuesCaptor.capture());
    final Map<String, String> values = valuesCaptor.getValue();
    assertAll(
        () -> assertEquals("ADMIN", values.get("role")),
        () -> assertEquals(NAME, values.get("name")),
        () -> assertEquals(EMAIL, values.get("email")));
  }

  // ── idempotencia

  @Test
  @DisplayName("execute() ignora un evento ya procesado y no envía otro correo")
  void shouldSkipAlreadyProcessedEvent() {
    // Arrange
    when(processedNotificationPort.isAlreadyProcessed(EVENT_ID)).thenReturn(true);

    // Act
    service.execute(command(NotificationType.USER_CREATED, Map.of()));

    // Assert
    verifyNoInteractions(emailTemplatePort, emailSenderPort);
    verify(processedNotificationPort, never()).markAsProcessed(any());
  }

  // ── fallo de envío

  @Test
  @DisplayName("execute() propaga el fallo de envío y no marca el evento como procesado")
  void shouldNotMarkProcessedWhenSendFails() {
    // Arrange
    when(processedNotificationPort.isAlreadyProcessed(EVENT_ID)).thenReturn(false);
    when(emailTemplatePort.render(any(), anyMap())).thenReturn(BODY);
    doThrow(EmailSenderException.becauseApiFailed(EVENT_ID, "HTTP 503"))
        .when(emailSenderPort)
        .send(any());

    // Act & Assert
    assertThrows(
        EmailSenderException.class,
        () -> service.execute(command(NotificationType.USER_CREATED, Map.of())));
    verify(processedNotificationPort, never()).markAsProcessed(any());
  }

  // ── validación del command

  @Test
  @DisplayName("execute() lanza ConstraintViolationException cuando el correo no es válido")
  void shouldThrowWhenCommandIsInvalid() {
    // Arrange
    final SendEmailNotificationCommand invalid =
        new SendEmailNotificationCommand(
            EVENT_ID, NotificationType.USER_CREATED, "no-es-email", NAME, Map.of());

    // Act & Assert
    assertThrows(ConstraintViolationException.class, () -> service.execute(invalid));
    verifyNoInteractions(processedNotificationPort, emailTemplatePort, emailSenderPort);
  }

  private static SendEmailNotificationCommand command(
      final NotificationType type, final Map<String, String> data) {
    return new SendEmailNotificationCommand(EVENT_ID, type, EMAIL, NAME, data);
  }
}
