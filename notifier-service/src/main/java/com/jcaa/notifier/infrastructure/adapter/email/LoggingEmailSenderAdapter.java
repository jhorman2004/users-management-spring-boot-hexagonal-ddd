package com.jcaa.notifier.infrastructure.adapter.email;

import com.jcaa.notifier.application.port.out.EmailSenderPort;
import com.jcaa.notifier.domain.model.EmailDestinationModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Solo para el perfil {@code local}: no envía nada, registra que el correo habría salido. Permite
 * probar el flujo completo con Kafka sin una API key de Brevo.
 */
@Slf4j
@Component
@Profile("local")
public class LoggingEmailSenderAdapter implements EmailSenderPort {

  private static final String LOG_NOT_SENT =
      "[LoggingEmailSenderAdapter] perfil local: correo NO enviado. referenceId={} asunto={}";

  @Override
  public void send(final EmailDestinationModel destination) {
    log.info(LOG_NOT_SENT, destination.getReferenceId(), destination.getSubject());
  }
}
