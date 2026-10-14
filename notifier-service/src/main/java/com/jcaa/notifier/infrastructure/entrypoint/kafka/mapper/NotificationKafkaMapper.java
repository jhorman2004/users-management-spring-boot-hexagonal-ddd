package com.jcaa.notifier.infrastructure.entrypoint.kafka.mapper;

import com.jcaa.events.notification.EmailNotificationEvent;
import com.jcaa.notifier.application.service.dto.command.SendEmailNotificationCommand;
import lombok.experimental.UtilityClass;

@UtilityClass
public class NotificationKafkaMapper {

  public SendEmailNotificationCommand toCommand(final EmailNotificationEvent event) {
    return new SendEmailNotificationCommand(
        event.eventId(),
        event.type(),
        event.recipientEmail(),
        event.recipientName(),
        event.data());
  }
}
