package com.jcaa.usersmanagement.infrastructure.adapter.messaging;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Solo para el plan gratuito de Render: notifier-service se duerme cuando no recibe tráfico HTTP
 * y, dormido, no lee Kafka (los mensajes esperan en el tópico, no se pierden). Después de publicar
 * un mensaje se hace un GET a su health check, sin esperar la respuesta, para despertarlo.
 *
 * <p>Si {@code NOTIFIER_WAKEUP_URL} está vacía (en local o en un plan pago) no hace nada. Mientras
 * un aviso está en curso no se lanza otro.
 */
@Slf4j
@Component
public class NotifierWakeUpClient implements DisposableBean {

  private static final String PROP_WAKE_UP_URL = "${notifier.wake-up-url:}";
  /** El arranque en frío de Render puede tardar cerca de un minuto. */
  private static final Duration WAKE_UP_TIMEOUT = Duration.ofSeconds(90);

  private static final String LOG_WAKE_UP_FAILED =
      "[NotifierWakeUpClient] no se pudo despertar a notifier-service: {}";

  private final String wakeUpUrl;
  private final RestClient restClient;
  private final ExecutorService executor;
  private final AtomicBoolean wakeUpInProgress = new AtomicBoolean(false);

  @Autowired
  public NotifierWakeUpClient(@Value(PROP_WAKE_UP_URL) final String wakeUpUrl) {
    this(wakeUpUrl, buildRestClient(), Executors.newSingleThreadExecutor());
  }

  NotifierWakeUpClient(
      final String wakeUpUrl, final RestClient restClient, final ExecutorService executor) {
    this.wakeUpUrl = wakeUpUrl.strip();
    this.restClient = restClient;
    this.executor = executor;
  }

  public void wakeUp() {
    if (wakeUpUrl.isEmpty() || !wakeUpInProgress.compareAndSet(false, true)) {
      return;
    }
    executor.execute(this::ping);
  }

  private void ping() {
    try {
      restClient.get().uri(wakeUpUrl).retrieve().toBodilessEntity();
    } catch (final RestClientException exception) {
      log.warn(LOG_WAKE_UP_FAILED, exception.getClass().getSimpleName());
    } finally {
      wakeUpInProgress.set(false);
    }
  }

  @Override
  public void destroy() {
    executor.shutdownNow();
  }

  private static RestClient buildRestClient() {
    final SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(WAKE_UP_TIMEOUT);
    requestFactory.setReadTimeout(WAKE_UP_TIMEOUT);
    return RestClient.builder().requestFactory(requestFactory).build();
  }
}
