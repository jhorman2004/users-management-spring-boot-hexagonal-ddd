package com.jcaa.accounts.infrastructure.adapter.messaging.mapper;

import com.jcaa.accounts.domain.model.AccountModel;
import com.jcaa.events.notification.EmailNotificationEvent;
import com.jcaa.events.notification.NotificationType;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AccountMessagingMapper {

  private static final String KEY_ACCOUNT_ID = "accountId";
  private static final String KEY_USER_ID = "userId";
  private static final String KEY_STATUS = "status";

  public EmailNotificationEvent toAccountCreatedEvent(
      final AccountModel account, final String recipientEmail, final String recipientName) {
    return EmailNotificationEvent.of(
        NotificationType.ACCOUNT_CREATED,
        recipientEmail,
        recipientName,
        Map.of(
            KEY_ACCOUNT_ID, account.getId().value(),
            KEY_USER_ID, account.getUserId().value(),
            KEY_STATUS, account.getStatus().name()));
  }
}
