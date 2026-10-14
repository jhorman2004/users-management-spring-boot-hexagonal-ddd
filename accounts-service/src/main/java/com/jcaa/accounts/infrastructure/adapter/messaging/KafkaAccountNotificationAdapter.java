package com.jcaa.accounts.infrastructure.adapter.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcaa.accounts.application.port.out.AccountNotificationPort;
import com.jcaa.accounts.domain.model.AccountModel;
import com.jcaa.accounts.infrastructure.adapter.messaging.mapper.AccountMessagingMapper;
import com.jcaa.events.notification.EmailNotificationEvent;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Paso 7 del diagrama: publica el mensaje "cuenta creada" en el tópico de notificaciones, con el ID
 * del usuario como clave. No bloquea ni propaga fallos: la cuenta ya quedó guardada.
 */
@Slf4j
@Component
public class KafkaAccountNotificationAdapter implements AccountNotificationPort {

  private static final String PROP_TOPIC = "${notifications.topic}";

  private static final String LOG_PUBLISHED =
      "[KafkaAccountNotificationAdapter] mensaje publicado. eventId={} type={}";
  private static final String LOG_PUBLISH_FAILED =
      "[KafkaAccountNotificationAdapter] no se pudo publicar el mensaje. eventId={} type={} causa={}";

  private final KafkaTemplate<String, String> kafkaTemplate;
  private final ObjectMapper objectMapper;
  private final String topic;

  public KafkaAccountNotificationAdapter(
      final KafkaTemplate<String, String> kafkaTemplate,
      final ObjectMapper objectMapper,
      @Value(PROP_TOPIC) final String topic) {
    this.kafkaTemplate = kafkaTemplate;
    this.objectMapper = objectMapper;
    this.topic = topic;
  }

  @Override
  public void notifyAccountCreated(
      final AccountModel account, final String recipientEmail, final String recipientName) {
    final EmailNotificationEvent event =
        AccountMessagingMapper.toAccountCreatedEvent(account, recipientEmail, recipientName);
    final String payload;
    try {
      payload = objectMapper.writeValueAsString(event);
    } catch (final JsonProcessingException exception) {
      logFailure(event, exception);
      return;
    }
    try {
      kafkaTemplate
          .send(topic, account.getUserId().value(), payload)
          .whenComplete(
              (result, exception) -> {
                if (Objects.isNull(exception)) {
                  log.info(LOG_PUBLISHED, event.eventId(), event.type());
                } else {
                  logFailure(event, exception);
                }
              });
    } catch (final RuntimeException exception) {
      // KafkaTemplate puede fallar antes de devolver el future (p. ej. sin conexión al broker).
      logFailure(event, exception);
    }
  }

  private static void logFailure(final EmailNotificationEvent event, final Throwable exception) {
    log.error(
        LOG_PUBLISH_FAILED, event.eventId(), event.type(), exception.getClass().getSimpleName());
  }
}
