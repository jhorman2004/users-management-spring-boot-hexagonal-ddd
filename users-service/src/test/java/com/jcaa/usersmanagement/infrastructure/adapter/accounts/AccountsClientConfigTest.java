package com.jcaa.usersmanagement.infrastructure.adapter.accounts;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AccountsClientConfig")
class AccountsClientConfigTest {

  private static final String VALID_KEY = "test-internal-api-key-0123456789";

  @Test
  @DisplayName("acepta una configuración válida")
  void shouldAcceptValidConfig() {
    // Act & Assert
    assertDoesNotThrow(() -> config("http://localhost:8081", VALID_KEY));
  }

  @Test
  @DisplayName("rechaza una URL vacía")
  void shouldRejectBlankBaseUrl() {
    // Act & Assert
    assertThrows(IllegalArgumentException.class, () -> config(" ", VALID_KEY));
  }

  @Test
  @DisplayName("rechaza una clave interna ausente o corta")
  void shouldRejectShortApiKey() {
    // Act & Assert
    assertThrows(IllegalArgumentException.class, () -> config("http://localhost:8081", "corta"));
  }

  @Test
  @DisplayName("toString() no expone la clave interna")
  void shouldNotExposeApiKeyInToString() {
    // Act
    final String text = config("http://localhost:8081", VALID_KEY).toString();

    // Assert
    assertFalse(text.contains(VALID_KEY));
  }

  private static AccountsClientConfig config(final String baseUrl, final String apiKey) {
    return new AccountsClientConfig(
        baseUrl,
        apiKey,
        Duration.ofSeconds(1),
        Duration.ofSeconds(1),
        3,
        Duration.ofMillis(10),
        2.0,
        50,
        10,
        5,
        Duration.ofSeconds(30));
  }
}
