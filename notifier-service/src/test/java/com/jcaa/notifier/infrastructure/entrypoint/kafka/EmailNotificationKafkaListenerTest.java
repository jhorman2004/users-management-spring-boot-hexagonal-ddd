package com.jcaa.notifier.infrastructure.entrypoint.kafka;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.jcaa.events.notification.EmailNotificationEvent;
import com.jcaa.events.notification.NotificationType;
import com.jcaa.notifier.application.port.in.SendEmailNotificationUseCase;
import com.jcaa.notifier.application.service.dto.command.SendEmailNotificationCommand;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("EmailNotificationKafkaListener")
@ExtendWith(MockitoExtension.class)
class EmailNotificationKafkaListenerTest {

  @Mock private SendEmailNotificationUseCase sendEmailNotificationUseCase;
  @Captor private ArgumentCaptor<SendEmailNotificationCommand> commandCaptor;

  private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
  private EmailNotificationKafkaListener listener;

  @BeforeEach
  void setUp() {
    listener = new EmailNotificationKafkaListener(sendEmailNotificationUseCase, objectMapper);
  }

  @Test
  @DisplayName("convierte el JSON del mensaje en un comando para el caso de uso")
  void shouldMapMessageToCommand() throws Exception {
    // Arrange
    final EmailNotificationEvent event =
        EmailNotificationEvent.of(
            NotificationType.ACCOUNT_CREATED,
            "john@example.com",
            "John Arrieta",
            Map.of("accountId", "acc-1"));
    final String payload = objectMapper.writeValueAsString(event);

    // Act
    listener.onMessage(payload);

    // Assert
    verify(sendEmailNotificationUseCase).execute(commandCaptor.capture());
    final SendEmailNotificationCommand command = commandCaptor.getValue();
    assertAll(
        () -> assertEquals(event.eventId(), command.eventId()),
        () -> assertEquals(NotificationType.ACCOUNT_CREATED, command.type()),
        () -> assertEquals("john@example.com", command.recipientEmail()),
        () -> assertEquals("acc-1", command.data().get("accountId")));
  }

  @Test
  @DisplayName("lanza InvalidNotificationMessageException con un JSON mal formado")
  void shouldRejectMalformedJson() {
    // Act & Assert
    assertThrows(InvalidNotificationMessageException.class, () -> listener.onMessage("{no-json"));
    verifyNoInteractions(sendEmailNotificationUseCase);
  }

  @Test
  @DisplayName("lanza InvalidNotificationMessageException cuando faltan campos obligatorios")
  void shouldRejectIncompleteEvent() {
    // Act & Assert
    assertThrows(
        InvalidNotificationMessageException.class,
        () -> listener.onMessage("{\"eventId\":\"e-1\",\"type\":\"USER_CREATED\"}"));
    verifyNoInteractions(sendEmailNotificationUseCase);
  }
}
