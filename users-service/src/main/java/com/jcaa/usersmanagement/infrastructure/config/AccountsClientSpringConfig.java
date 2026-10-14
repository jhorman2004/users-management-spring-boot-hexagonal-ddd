package com.jcaa.usersmanagement.infrastructure.config;

import com.jcaa.usersmanagement.infrastructure.adapter.accounts.AccountsClientConfig;
import com.jcaa.usersmanagement.infrastructure.adapter.accounts.AccountsRestClientAdapter;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Slf4j
@Configuration(proxyBeanMethods = false)
public class AccountsClientSpringConfig {

  private static final String LOG_CONFIG_LOADED =
      "[AccountsClientSpringConfig] cliente de accounts listo. baseUrl={} readTimeout={}";

  @Bean
  public AccountsClientConfig accountsClientConfig(
      @Value("${accounts.base-url}") final String baseUrl,
      @Value("${accounts.api-key}") final String apiKey,
      @Value("${accounts.timeout.connect}") final Duration connectTimeout,
      @Value("${accounts.timeout.read}") final Duration readTimeout,
      @Value("${accounts.retry.max-attempts}") final int retryMaxAttempts,
      @Value("${accounts.retry.initial-delay}") final Duration retryInitialDelay,
      @Value("${accounts.retry.multiplier}") final double retryMultiplier,
      @Value("${accounts.circuit-breaker.failure-rate-threshold}") final int circuitFailureThreshold,
      @Value("${accounts.circuit-breaker.sliding-window-size}") final int circuitSlidingWindowSize,
      @Value("${accounts.circuit-breaker.minimum-calls}") final int circuitMinimumCalls,
      @Value("${accounts.circuit-breaker.open-duration}") final Duration circuitOpenDuration) {
    final AccountsClientConfig config =
        new AccountsClientConfig(
            baseUrl,
            apiKey,
            connectTimeout,
            readTimeout,
            retryMaxAttempts,
            retryInitialDelay,
            retryMultiplier,
            circuitFailureThreshold,
            circuitSlidingWindowSize,
            circuitMinimumCalls,
            circuitOpenDuration);
    log.info(LOG_CONFIG_LOADED, config.baseUrl(), config.readTimeout());
    return config;
  }

  @Bean
  public RestClient accountsRestClient(final AccountsClientConfig config) {
    final SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(config.connectTimeout());
    requestFactory.setReadTimeout(config.readTimeout());
    return RestClient.builder()
        .baseUrl(config.baseUrl())
        .requestFactory(requestFactory)
        .defaultHeader(AccountsRestClientAdapter.API_KEY_HEADER, config.apiKey())
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build();
  }
}
