package com.jcaa.notifier.application.service;

import com.jcaa.events.notification.NotificationType;
import com.jcaa.notifier.application.port.in.SendEmailNotificationUseCase;
import com.jcaa.notifier.application.port.out.EmailSenderPort;
import com.jcaa.notifier.application.port.out.EmailTemplatePort;
import com.jcaa.notifier.application.port.out.ProcessedNotificationPort;
import com.jcaa.notifier.application.service.dto.command.SendEmailNotificationCommand;
import com.jcaa.notifier.domain.model.EmailDestinationModel;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Convierte un mensaje "nuevo correo" en un correo real: elige la plantilla y el asunto según el
 * tipo, la rellena y la envía. Ignora los mensajes repetidos (mismo {@code eventId}).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SendEmailNotificationService implements SendEmailNotificationUseCase {

  private static final String SUBJECT_USER_CREATED =
      "Tu cuenta ha sido creada — Gestion de Usuarios";
  private static final String SUBJECT_USER_UPDATED =
      "Tu cuenta ha sido actualizada — Gestion de Usuarios";
  private static final String SUBJECT_ACCOUNT_CREATED =
      "Tu cuenta de acceso está lista — Gestion de Usuarios";

  private static final String TEMPLATE_USER_CREATED = "user-created.html";
  private static final String TEMPLATE_USER_UPDATED = "user-updated.html";
  private static final String TEMPLATE_ACCOUNT_CREATED = "account-created.html";

  private static final String TOKEN_NAME = "name";
  private static final String TOKEN_EMAIL = "email";

  private static final String LOG_DUPLICATE =
      "[SendEmailNotificationService] evento ya procesado, se ignora. eventId={} type={}";
  private static final String LOG_SENT =
      "[SendEmailNotificationService] notificación enviada. eventId={} type={}";

  private final EmailTemplatePort emailTemplatePort;
  private final EmailSenderPort emailSenderPort;
  private final ProcessedNotificationPort processedNotificationPort;
  private final Validator validator;

  @Override
  public void execute(final SendEmailNotificationCommand command) {
    validateCommand(command);

    if (processedNotificationPort.isAlreadyProcessed(command.eventId())) {
      log.info(LOG_DUPLICATE, command.eventId(), command.type());
      return;
    }

    final TemplateSpec template = templateFor(command.type());
    final String body = emailTemplatePort.render(template.fileName(), templateValues(command));
    emailSenderPort.send(
        new EmailDestinationModel(
            command.eventId(),
            command.recipientEmail(),
            command.recipientName(),
            template.subject(),
            body));

    processedNotificationPort.markAsProcessed(command.eventId());
    log.info(LOG_SENT, command.eventId(), command.type());
  }

  private void validateCommand(final SendEmailNotificationCommand command) {
    final Set<ConstraintViolation<SendEmailNotificationCommand>> violations =
        validator.validate(command);
    if (!violations.isEmpty()) {
      throw new ConstraintViolationException(violations);
    }
  }

  private static TemplateSpec templateFor(final NotificationType type) {
    return switch (type) {
      case USER_CREATED -> new TemplateSpec(TEMPLATE_USER_CREATED, SUBJECT_USER_CREATED);
      case USER_UPDATED -> new TemplateSpec(TEMPLATE_USER_UPDATED, SUBJECT_USER_UPDATED);
      case ACCOUNT_CREATED -> new TemplateSpec(TEMPLATE_ACCOUNT_CREATED, SUBJECT_ACCOUNT_CREATED);
    };
  }

  /** Los datos del evento, más el nombre y el correo del destinatario si el evento no los trae. */
  private static Map<String, String> templateValues(final SendEmailNotificationCommand command) {
    final Map<String, String> values = new HashMap<>(command.data());
    values.putIfAbsent(TOKEN_NAME, command.recipientName());
    values.putIfAbsent(TOKEN_EMAIL, command.recipientEmail());
    return values;
  }

  private record TemplateSpec(String fileName, String subject) {}
}
