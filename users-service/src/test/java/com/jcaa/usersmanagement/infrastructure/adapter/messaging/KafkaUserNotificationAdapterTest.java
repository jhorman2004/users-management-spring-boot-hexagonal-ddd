package com.jcaa.usersmanagement.infrastructure.adapter.messaging;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.jcaa.events.notification.EmailNotificationEvent;
import com.jcaa.events.notification.NotificationType;
import com.jcaa.usersmanagement.domain.enums.UserRole;
import com.jcaa.usersmanagement.domain.enums.UserStatus;
import com.jcaa.usersmanagement.domain.model.UserModel;
import com.jcaa.usersmanagement.domain.valueobject.UserEmail;
import com.jcaa.usersmanagement.domain.valueobject.UserId;
import com.jcaa.usersmanagement.domain.valueobject.UserName;
import com.jcaa.usersmanagement.domain.valueobject.UserPassword;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.common.errors.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Tests para KafkaUserNotificationAdapter.
 *
 * <p>Cubre: contenido del mensaje publicado (tipo, destinatario, datos, sin contraseña), clave del
 * mensaje y que un fallo de Kafka no se propaga al caso de uso.
 */
@DisplayName("KafkaUserNotificationAdapter")
@ExtendWith(MockitoExtension.class)
class KafkaUserNotificationAdapterTest {

  private static final String TOPIC = "notifications.email.test";
  private static final String USER_ID = "u-001";
  private static final String EMAIL = "john@example.com";

  @Mock private KafkaTemplate<String, String> kafkaTemplate;
  @Mock private NotifierWakeUpClient notifierWakeUpClient;
  @Captor private ArgumentCaptor<String> payloadCaptor;

  private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
  private KafkaUserNotificationAdapter adapter;
  private UserModel user;

  @BeforeEach
  void setUp() {
    adapter =
        new KafkaUserNotificationAdapter(kafkaTemplate, objectMapper, notifierWakeUpClient, TOPIC);
    user =
        new UserModel(
            new UserId(USER_ID),
            new UserName("John Arrieta"),
            new UserEmail(EMAIL),
            UserPassword.fromHash("$2a$12$hash"),
            UserRole.ADMIN,
            UserStatus.PENDING);
  }

  @Test
  @DisplayName("notifyUserCreated() publica un USER_CREATED con la clave del usuario")
  void shouldPublishUserCreatedEvent() throws Exception {
    // Arrange
    when(kafkaTemplate.send(anyString(), anyString(), anyString()))
        .thenReturn(CompletableFuture.completedFuture(null));

    // Act
    adapter.notifyUserCreated(user);

    // Assert
    verify(kafkaTemplate).send(eq(TOPIC), eq(USER_ID), payloadCaptor.capture());
    final EmailNotificationEvent event =
        objectMapper.readValue(payloadCaptor.getValue(), EmailNotificationEvent.class);
    assertAll(
        () -> assertEquals(NotificationType.USER_CREATED, event.type()),
        () -> assertEquals(EMAIL, event.recipientEmail()),
        () -> assertEquals("ADMIN", event.data().get("role")),
        () -> assertEquals("PENDING", event.data().get("status")),
        () -> assertFalse(payloadCaptor.getValue().contains("$2a$"), "nunca el hash ni la clave"));
    verify(notifierWakeUpClient).wakeUp();
  }

  @Test
  @DisplayName("notifyUserUpdated() publica un USER_UPDATED")
  void shouldPublishUserUpdatedEvent() throws Exception {
    // Arrange
    when(kafkaTemplate.send(anyString(), anyString(), anyString()))
        .thenReturn(CompletableFuture.completedFuture(null));

    // Act
    adapter.notifyUserUpdated(user);

    // Assert
    verify(kafkaTemplate).send(eq(TOPIC), eq(USER_ID), payloadCaptor.capture());
    assertEquals(
        NotificationType.USER_UPDATED,
        objectMapper.readValue(payloadCaptor.getValue(), EmailNotificationEvent.class).type());
  }

  @Test
  @DisplayName("no propaga el error cuando Kafka falla al enviar")
  void shouldNotPropagateSendFailure() {
    // Arrange
    when(kafkaTemplate.send(anyString(), anyString(), any()))
        .thenThrow(new TimeoutException("broker no disponible"));

    // Act & Assert
    assertDoesNotThrow(() -> adapter.notifyUserCreated(user));
    verify(notifierWakeUpClient, never()).wakeUp();
  }

  @Test
  @DisplayName("no propaga el error cuando el envío termina con fallo")
  void shouldNotPropagateAsyncFailure() {
    // Arrange
    when(kafkaTemplate.send(anyString(), anyString(), anyString()))
        .thenReturn(CompletableFuture.failedFuture(new TimeoutException("sin respuesta")));

    // Act & Assert
    assertDoesNotThrow(() -> adapter.notifyUserCreated(user));
    verify(notifierWakeUpClient, never()).wakeUp();
  }
}
