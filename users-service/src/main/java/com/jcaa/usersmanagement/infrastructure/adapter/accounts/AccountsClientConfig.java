package com.jcaa.usersmanagement.infrastructure.adapter.accounts;

import java.time.Duration;
import java.util.Objects;

/** Configuración del cliente HTTP hacia accounts-service. Se valida al arrancar. */
public record AccountsClientConfig(
    String baseUrl,
    String apiKey,
    Duration connectTimeout,
    Duration readTimeout,
    int retryMaxAttempts,
    Duration retryInitialDelay,
    double retryMultiplier,
    int circuitFailureThreshold,
    int circuitSlidingWindowSize,
    int circuitMinimumCalls,
    Duration circuitOpenDuration) {

  private static final int MINIMUM_API_KEY_LENGTH = 16;

  public AccountsClientConfig {
    if (Objects.isNull(baseUrl) || baseUrl.isBlank()) {
      throw new IllegalArgumentException(
          "La configuracion requerida 'accounts.base-url / ACCOUNTS_BASE_URL' no puede estar vacia.");
    }
    if (Objects.isNull(apiKey) || apiKey.strip().length() < MINIMUM_API_KEY_LENGTH) {
      throw new IllegalArgumentException(
          "INTERNAL_API_KEY es obligatoria y debe tener al menos 16 caracteres.");
    }
    apiKey = apiKey.strip();
  }

  /** Oculta la clave para que nunca termine en un log. */
  @Override
  public String toString() {
    return "AccountsClientConfig[baseUrl=" + baseUrl + ", readTimeout=" + readTimeout + "]";
  }
}
