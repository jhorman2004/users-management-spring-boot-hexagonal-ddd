package com.jcaa.notifier.application.port.out;

/** Registro de eventos ya enviados, para no repetir un correo si Kafka entrega el mensaje dos veces. */
public interface ProcessedNotificationPort {
  boolean isAlreadyProcessed(String eventId);

  void markAsProcessed(String eventId);
}
