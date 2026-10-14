package com.jcaa.usersmanagement.infrastructure.adapter.messaging.mapper;

import com.jcaa.events.notification.EmailNotificationEvent;
import com.jcaa.events.notification.NotificationType;
import com.jcaa.usersmanagement.domain.event.UserCreatedDomainEvent;
import com.jcaa.usersmanagement.domain.event.UserUpdatedDomainEvent;
import com.jcaa.usersmanagement.domain.model.UserModel;
import lombok.experimental.UtilityClass;

/** Traduce los eventos de dominio del usuario al mensaje "nuevo correo" del tópico. */
@UtilityClass
public class UserMessagingMapper {

  public EmailNotificationEvent toUserCreatedEvent(final UserModel user) {
    return EmailNotificationEvent.of(
        NotificationType.USER_CREATED,
        user.getEmail().value(),
        user.getName().value(),
        new UserCreatedDomainEvent(user).payload());
  }

  public EmailNotificationEvent toUserUpdatedEvent(final UserModel user) {
    return EmailNotificationEvent.of(
        NotificationType.USER_UPDATED,
        user.getEmail().value(),
        user.getName().value(),
        new UserUpdatedDomainEvent(user).payload());
  }
}
