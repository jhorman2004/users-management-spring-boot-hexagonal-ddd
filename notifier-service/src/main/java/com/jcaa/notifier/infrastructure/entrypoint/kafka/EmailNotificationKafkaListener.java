package com.jcaa.notifier.infrastructure.entrypoint.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcaa.events.notification.EmailNotificationEvent;
import com.jcaa.notifier.application.port.in.SendEmailNotificationUseCase;
import com.jcaa.notifier.infrastructure.entrypoint.kafka.mapper.NotificationKafkaMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Paso 3 del diagrama: recibe del tópico cada mensaje "nuevo correo" y lo entrega al caso de uso.
 * Si el caso de uso falla, el error handler reintenta y, al agotar los intentos, manda el mensaje a
 * la DLT.
 */
@Component
@RequiredArgsConstructor
public class EmailNotificationKafkaListener {

  private final SendEmailNotificationUseCase sendEmailNotificationUseCase;
  private final ObjectMapper objectMapper;

  @KafkaListener(topics = "${notifications.topic}")
  public void onMessage(final String payload) {
    final EmailNotificationEvent event = readEvent(payload);
    sendEmailNotificationUseCase.execute(NotificationKafkaMapper.toCommand(event));
  }

  private EmailNotificationEvent readEvent(final String payload) {
    try {
      return objectMapper.readValue(payload, EmailNotificationEvent.class);
    } catch (final JsonProcessingException exception) {
      throw InvalidNotificationMessageException.becausePayloadIsNotValid(exception);
    }
  }
}
