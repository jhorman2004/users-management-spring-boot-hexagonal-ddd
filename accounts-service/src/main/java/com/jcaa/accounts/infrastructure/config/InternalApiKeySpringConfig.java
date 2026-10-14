package com.jcaa.accounts.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcaa.accounts.infrastructure.security.InternalApiKeyFilter;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Protege {@code /api/*} con la clave interna. {@code /actuator/health} queda libre para el health
 * check de Render. Si la clave falta o es corta, el servicio no arranca.
 */
@Configuration(proxyBeanMethods = false)
public class InternalApiKeySpringConfig {

  private static final String PROP_INTERNAL_API_KEY = "${internal.api-key}";
  private static final String PROTECTED_PATHS = "/api/*";
  private static final int MINIMUM_KEY_LENGTH = 16;
  private static final String MESSAGE_INVALID_KEY =
      "INTERNAL_API_KEY es obligatoria y debe tener al menos 16 caracteres.";

  @Bean
  public FilterRegistrationBean<InternalApiKeyFilter> internalApiKeyFilter(
      @Value(PROP_INTERNAL_API_KEY) final String internalApiKey, final ObjectMapper objectMapper) {
    if (Objects.isNull(internalApiKey) || internalApiKey.strip().length() < MINIMUM_KEY_LENGTH) {
      throw new IllegalStateException(MESSAGE_INVALID_KEY);
    }
    final FilterRegistrationBean<InternalApiKeyFilter> registration =
        new FilterRegistrationBean<>(new InternalApiKeyFilter(internalApiKey.strip(), objectMapper));
    registration.addUrlPatterns(PROTECTED_PATHS);
    return registration;
  }
}
