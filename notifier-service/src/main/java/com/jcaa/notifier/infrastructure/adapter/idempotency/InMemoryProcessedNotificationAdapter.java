package com.jcaa.notifier.infrastructure.adapter.idempotency;

import com.jcaa.notifier.application.port.out.ProcessedNotificationPort;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Recuerda los últimos {@code capacity} eventos enviados; al llenarse olvida el más antiguo.
 *
 * <p>Vive en memoria, así que se vacía al reiniciar. Cubre las reentregas normales de Kafka; para
 * el caso de un reinicio, Brevo recibe el mismo {@code eventId} como clave de idempotencia.
 */
@Component
public class InMemoryProcessedNotificationAdapter implements ProcessedNotificationPort {

  private static final String PROP_CACHE_SIZE = "${notifications.idempotency.cache-size}";

  private final Set<String> processedEventIds;

  public InMemoryProcessedNotificationAdapter(@Value(PROP_CACHE_SIZE) final int capacity) {
    this.processedEventIds =
        Collections.synchronizedSet(Collections.newSetFromMap(new BoundedMap(capacity)));
  }

  @Override
  public boolean isAlreadyProcessed(final String eventId) {
    return processedEventIds.contains(eventId);
  }

  @Override
  public void markAsProcessed(final String eventId) {
    processedEventIds.add(eventId);
  }

  private static final class BoundedMap extends LinkedHashMap<String, Boolean> {

    private final int capacity;

    private BoundedMap(final int capacity) {
      this.capacity = capacity;
    }

    @Override
    protected boolean removeEldestEntry(final Map.Entry<String, Boolean> eldest) {
      return size() > capacity;
    }
  }
}
