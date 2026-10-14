package com.jcaa.accounts.infrastructure.adapter.messaging;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.jcaa.accounts.domain.model.AccountModel;
import com.jcaa.accounts.domain.valueobject.AccountUserId;
import com.jcaa.events.notification.EmailNotificationEvent;
import com.jcaa.events.notification.NotificationType;
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

@DisplayName("KafkaAccountNotificationAdapter")
@ExtendWith(MockitoExtension.class)
class KafkaAccountNotificationAdapterTest {

  private static final String TOPIC = "notifications.email.test";
  private static final String USER_ID = "u-001";

  @Mock private KafkaTemplate<String, String> kafkaTemplate;
  @Captor private ArgumentCaptor<String> payloadCaptor;

  private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
  private KafkaAccountNotificationAdapter adapter;
  private AccountModel account;

  @BeforeEach
  void setUp() {
    adapter = new KafkaAccountNotificationAdapter(kafkaTemplate, objectMapper, TOPIC);
    account = AccountModel.openFor(new AccountUserId(USER_ID));
  }

  @Test
  @DisplayName("publica un ACCOUNT_CREATED con el id de la cuenta y la clave del usuario")
  void shouldPublishAccountCreatedEvent() throws Exception {
    // Arrange
    when(kafkaTemplate.send(anyString(), anyString(), anyString()))
        .thenReturn(CompletableFuture.completedFuture(null));

    // Act
    adapter.notifyAccountCreated(account, "john@example.com", "John Arrieta");

    // Assert
    verify(kafkaTemplate).send(eq(TOPIC), eq(USER_ID), payloadCaptor.capture());
    final EmailNotificationEvent event =
        objectMapper.readValue(payloadCaptor.getValue(), EmailNotificationEvent.class);
    assertAll(
        () -> assertEquals(NotificationType.ACCOUNT_CREATED, event.type()),
        () -> assertEquals("john@example.com", event.recipientEmail()),
        () -> assertEquals(account.getId().value(), event.data().get("accountId")),
        () -> assertEquals("ACTIVE", event.data().get("status")));
  }

  @Test
  @DisplayName("no propaga el error cuando Kafka falla")
  void shouldNotPropagateKafkaFailure() {
    // Arrange
    when(kafkaTemplate.send(anyString(), anyString(), anyString()))
        .thenThrow(new TimeoutException("broker no disponible"));

    // Act & Assert
    assertDoesNotThrow(
        () -> adapter.notifyAccountCreated(account, "john@example.com", "John Arrieta"));
  }
}
