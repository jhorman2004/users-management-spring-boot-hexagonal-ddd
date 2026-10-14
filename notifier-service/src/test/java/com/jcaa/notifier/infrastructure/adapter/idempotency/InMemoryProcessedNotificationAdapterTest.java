package com.jcaa.notifier.infrastructure.adapter.idempotency;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("InMemoryProcessedNotificationAdapter")
class InMemoryProcessedNotificationAdapterTest {

  @Test
  @DisplayName("recuerda los eventos marcados como procesados")
  void shouldRememberProcessedEvents() {
    // Arrange
    final InMemoryProcessedNotificationAdapter registry = new InMemoryProcessedNotificationAdapter(10);

    // Act
    registry.markAsProcessed("event-1");

    // Assert
    assertAll(
        () -> assertTrue(registry.isAlreadyProcessed("event-1")),
        () -> assertFalse(registry.isAlreadyProcessed("event-2")));
  }

  @Test
  @DisplayName("olvida el evento más antiguo al superar la capacidad")
  void shouldEvictOldestWhenFull() {
    // Arrange
    final InMemoryProcessedNotificationAdapter registry = new InMemoryProcessedNotificationAdapter(2);

    // Act
    registry.markAsProcessed("event-1");
    registry.markAsProcessed("event-2");
    registry.markAsProcessed("event-3");

    // Assert
    assertAll(
        () -> assertFalse(registry.isAlreadyProcessed("event-1")),
        () -> assertTrue(registry.isAlreadyProcessed("event-2")),
        () -> assertTrue(registry.isAlreadyProcessed("event-3")));
  }
}
