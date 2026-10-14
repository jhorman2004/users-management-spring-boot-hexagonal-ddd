package com.jcaa.usersmanagement.infrastructure.adapter.accounts;

import com.jcaa.usersmanagement.application.port.out.CreateAccountPort;
import com.jcaa.usersmanagement.domain.exception.AccountProvisioningException;
import com.jcaa.usersmanagement.domain.model.UserModel;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.util.Objects;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Paso 5 del diagrama: llamada HTTP síncrona a accounts-service.
 *
 * <p>Usa el mismo esquema de resiliencia que el cliente de Brevo: reintentos con backoff
 * exponencial solo ante fallos transitorios (timeout, 5xx, 408, 429) y circuit breaker para no
 * insistir cuando accounts está caído. Reintentar es seguro porque accounts es idempotente por
 * usuario.
 */
@Slf4j
@Component
public class AccountsRestClientAdapter implements CreateAccountPort {

  public static final String API_KEY_HEADER = "X-Internal-Api-Key";

  private static final String CREATE_ACCOUNT_PATH = "/api/accounts";
  private static final String RESILIENCE_NAME = "accounts-service";
  private static final String DETAIL_CIRCUIT_OPEN = "circuit breaker abierto";
  private static final String DETAIL_HTTP = "HTTP ";

  private static final String LOG_ACCOUNT_READY =
      "[AccountsRestClientAdapter] cuenta lista. userId={}";
  private static final String LOG_RETRY =
      "[AccountsRestClientAdapter] reintento {} tras fallo transitorio: {}";

  private final RestClient restClient;
  private final Retry retry;
  private final CircuitBreaker circuitBreaker;

  public AccountsRestClientAdapter(
      final AccountsClientConfig config, final RestClient accountsRestClient) {
    this.restClient = accountsRestClient;
    this.retry = buildRetry(config);
    this.circuitBreaker = buildCircuitBreaker(config);
    this.retry
        .getEventPublisher()
        .onRetry(
            event ->
                log.warn(
                    LOG_RETRY,
                    event.getNumberOfRetryAttempts(),
                    exceptionType(event.getLastThrowable())));
  }

  @Override
  public void createFor(final UserModel user) {
    final String userId = user.getId().value();
    final CreateAccountRequest request =
        new CreateAccountRequest(userId, user.getEmail().value(), user.getName().value());
    final Runnable protectedCall =
        Retry.decorateRunnable(
            retry, CircuitBreaker.decorateRunnable(circuitBreaker, () -> post(request)));
    try {
      protectedCall.run();
      log.info(LOG_ACCOUNT_READY, userId);
    } catch (final RestClientResponseException exception) {
      throw AccountProvisioningException.becauseAccountServiceFailed(
          userId, DETAIL_HTTP + exception.getStatusCode().value());
    } catch (final CallNotPermittedException exception) {
      throw AccountProvisioningException.becauseAccountServiceFailed(userId, DETAIL_CIRCUIT_OPEN);
    } catch (final RestClientException exception) {
      throw AccountProvisioningException.becauseAccountServiceFailed(
          userId, exception.getClass().getSimpleName());
    }
  }

  private void post(final CreateAccountRequest request) {
    restClient.post().uri(CREATE_ACCOUNT_PATH).body(request).retrieve().toBodilessEntity();
  }

  private static Retry buildRetry(final AccountsClientConfig config) {
    final IntervalFunction backoff =
        IntervalFunction.ofExponentialBackoff(
            config.retryInitialDelay().toMillis(), config.retryMultiplier());
    final RetryConfig retryConfig =
        RetryConfig.custom()
            .maxAttempts(config.retryMaxAttempts())
            .intervalFunction(backoff)
            .retryOnException(AccountsRestClientAdapter::isTransientFailure)
            .build();
    return Retry.of(RESILIENCE_NAME, retryConfig);
  }

  private static CircuitBreaker buildCircuitBreaker(final AccountsClientConfig config) {
    final Predicate<Throwable> transientFailure = AccountsRestClientAdapter::isTransientFailure;
    final CircuitBreakerConfig circuitConfig =
        CircuitBreakerConfig.custom()
            .failureRateThreshold(config.circuitFailureThreshold())
            .slidingWindowSize(config.circuitSlidingWindowSize())
            .minimumNumberOfCalls(config.circuitMinimumCalls())
            .waitDurationInOpenState(config.circuitOpenDuration())
            .recordException(transientFailure)
            .build();
    return CircuitBreaker.of(RESILIENCE_NAME, circuitConfig);
  }

  private static boolean isTransientFailure(final Throwable exception) {
    if (exception instanceof ResourceAccessException
        || exception instanceof HttpServerErrorException) {
      return true;
    }
    if (exception instanceof HttpClientErrorException clientError) {
      final HttpStatusCode status = clientError.getStatusCode();
      return status.value() == 408 || status.value() == 429;
    }
    return false;
  }

  private static String exceptionType(final Throwable exception) {
    return Objects.isNull(exception) ? "causa desconocida" : exception.getClass().getSimpleName();
  }
}
