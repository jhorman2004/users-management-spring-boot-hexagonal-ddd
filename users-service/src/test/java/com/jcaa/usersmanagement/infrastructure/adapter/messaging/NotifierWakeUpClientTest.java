package com.jcaa.usersmanagement.infrastructure.adapter.messaging;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@DisplayName("NotifierWakeUpClient")
class NotifierWakeUpClientTest {

  private static final String HEALTH_URL = "https://notifier.test/actuator/health";

  private RestClient.Builder restClientBuilder;
  private MockRestServiceServer server;
  private ExecutorService executor;

  @BeforeEach
  void setUp() {
    restClientBuilder = RestClient.builder();
    server = MockRestServiceServer.bindTo(restClientBuilder).build();
    executor = Executors.newSingleThreadExecutor();
  }

  @Test
  @DisplayName("wakeUp() hace un GET al health check del notifier")
  void shouldPingHealthCheck() throws InterruptedException {
    // Arrange
    server
        .expect(once(), requestTo(HEALTH_URL))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess());
    final NotifierWakeUpClient client =
        new NotifierWakeUpClient(HEALTH_URL, restClientBuilder.build(), executor);

    // Act
    client.wakeUp();
    awaitExecutor();

    // Assert
    server.verify();
  }

  @Test
  @DisplayName("wakeUp() no hace nada cuando la URL está vacía")
  void shouldDoNothingWithoutUrl() throws InterruptedException {
    // Arrange
    final NotifierWakeUpClient client =
        new NotifierWakeUpClient(" ", restClientBuilder.build(), executor);

    // Act
    client.wakeUp();
    awaitExecutor();

    // Assert — sin expectativas: cualquier petición haría fallar verify()
    server.verify();
  }

  @Test
  @DisplayName("wakeUp() no propaga errores del notifier")
  void shouldIgnoreErrors() throws InterruptedException {
    // Arrange
    server.expect(once(), requestTo(HEALTH_URL)).andRespond(withServerError());
    final NotifierWakeUpClient client =
        new NotifierWakeUpClient(HEALTH_URL, restClientBuilder.build(), executor);

    // Act & Assert
    assertDoesNotThrow(client::wakeUp);
    awaitExecutor();
    server.verify();
  }

  private void awaitExecutor() throws InterruptedException {
    executor.shutdown();
    assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "el aviso debe terminar");
  }
}
