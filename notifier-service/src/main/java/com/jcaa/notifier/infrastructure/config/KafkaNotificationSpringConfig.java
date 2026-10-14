package com.jcaa.notifier.infrastructure.config;

import com.jcaa.notifier.infrastructure.entrypoint.kafka.InvalidNotificationMessageException;
import jakarta.validation.ConstraintViolationException;
import java.time.Duration;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration(proxyBeanMethods = false)
public class KafkaNotificationSpringConfig {

  private static final String PROP_TOPIC = "${notifications.topic}";
  private static final String PROP_DLT_TOPIC = "${notifications.dlt-topic}";
  private static final String PROP_RETRY_ATTEMPTS = "${notifications.retry.attempts}";
  private static final String PROP_RETRY_INTERVAL = "${notifications.retry.interval}";
  private static final String PROP_CREATE_TOPICS = "notifications.create-topics";

  /** Sin partición fija: Kafka elige la de la DLT aunque tenga menos particiones que el tópico. */
  private static final int ANY_PARTITION = -1;

  private static final int LOCAL_PARTITIONS = 1;
  private static final int LOCAL_REPLICAS = 1;

  /**
   * Reintenta los mensajes que fallan y, al agotar los intentos, los publica en la DLT. Los
   * mensajes mal formados o inválidos no se reintentan: van directo a la DLT.
   */
  @Bean
  public DefaultErrorHandler kafkaErrorHandler(
      final KafkaOperations<Object, Object> kafkaOperations,
      @Value(PROP_DLT_TOPIC) final String dltTopic,
      @Value(PROP_RETRY_ATTEMPTS) final long retryAttempts,
      @Value(PROP_RETRY_INTERVAL) final Duration retryInterval) {
    final DeadLetterPublishingRecoverer recoverer =
        new DeadLetterPublishingRecoverer(
            kafkaOperations, (record, exception) -> new TopicPartition(dltTopic, ANY_PARTITION));
    final DefaultErrorHandler errorHandler =
        new DefaultErrorHandler(
            recoverer, new FixedBackOff(retryInterval.toMillis(), retryAttempts));
    errorHandler.addNotRetryableExceptions(
        InvalidNotificationMessageException.class, ConstraintViolationException.class);
    return errorHandler;
  }

  /** Solo en local: en Aiven los tópicos se crean desde la consola. */
  @Bean
  @ConditionalOnProperty(name = PROP_CREATE_TOPICS, havingValue = "true")
  public NewTopic notificationsTopic(@Value(PROP_TOPIC) final String topic) {
    return TopicBuilder.name(topic).partitions(LOCAL_PARTITIONS).replicas(LOCAL_REPLICAS).build();
  }

  @Bean
  @ConditionalOnProperty(name = PROP_CREATE_TOPICS, havingValue = "true")
  public NewTopic notificationsDltTopic(@Value(PROP_DLT_TOPIC) final String dltTopic) {
    return TopicBuilder.name(dltTopic)
        .partitions(LOCAL_PARTITIONS)
        .replicas(LOCAL_REPLICAS)
        .build();
  }
}
