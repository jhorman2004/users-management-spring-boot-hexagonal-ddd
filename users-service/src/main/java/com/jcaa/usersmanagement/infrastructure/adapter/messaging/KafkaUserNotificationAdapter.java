package com.jcaa.usersmanagement.infrastructure.adapter.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcaa.events.notification.EmailNotificationEvent;
import com.jcaa.usersmanagement.application.port.out.UserNotificationPort;
import com.jcaa.usersmanagement.domain.model.UserModel;
import com.jcaa.usersmanagement.infrastructure.adapter.messaging.mapper.UserMessagingMapper;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Paso 2 del diagrama: publica en el tópico de notificaciones el mensaje "nuevo correo". La clave
 * del mensaje es el ID del usuario, así los mensajes de un mismo usuario conservan su orden.
 *
 * <p>No bloquea ni propaga fallos: si Kafka no está disponible se registra el error y el usuario
 * igual queda guardado, como pasaba antes cuando el correo se enviaba en un hilo aparte.
 */
@Slf4j
@Component
public class KafkaUserNotificationAdapter implements UserNotificationPort {

  private static final String PROP_TOPIC = "${notifications.topic}";

  private static final String LOG_PUBLISHED =
      "[KafkaUserNotificationAdapter] mensaje publicado. eventId={} type={}";
  private static final String LOG_PUBLISH_FAILED =
      "[KafkaUserNotificationAdapter] no se pudo publicar el mensaje. eventId={} type={} causa={}";

  private final KafkaTemplate<String, String> kafkaTemplate;
  private final ObjectMapper objectMapper;
  private final NotifierWakeUpClient notifierWakeUpClient;
  private final String topic;

  public KafkaUserNotificationAdapter(
      final KafkaTemplate<String, String> kafkaTemplate,
      final ObjectMapper objectMapper,
      final NotifierWakeUpClient notifierWakeUpClient,
      @Value(PROP_TOPIC) final String topic) {
    this.kafkaTemplate = kafkaTemplate;
    this.objectMapper = objectMapper;
    this.notifierWakeUpClient = notifierWakeUpClient;
    this.topic = topic;
  }

  @Override
  public void notifyUserCreated(final UserModel user) {
    publish(user.getId().value(), UserMessagingMapper.toUserCreatedEvent(user));
  }

  @Override
  public void notifyUserUpdated(final UserModel user) {
    publish(user.getId().value(), UserMessagingMapper.toUserUpdatedEvent(user));
  }

  private void publish(final String key, final EmailNotificationEvent event) {
    final String payload;
    try {
      payload = objectMapper.writeValueAsString(event);
    } catch (final JsonProcessingException exception) {
      logFailure(event, exception);
      return;
    }
    try {
      kafkaTemplate
          .send(topic, key, payload)
          .whenComplete(
              (result, exception) -> {
                if (Objects.isNull(exception)) {
                  log.info(LOG_PUBLISHED, event.eventId(), event.type());
                  notifierWakeUpClient.wakeUp();
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
