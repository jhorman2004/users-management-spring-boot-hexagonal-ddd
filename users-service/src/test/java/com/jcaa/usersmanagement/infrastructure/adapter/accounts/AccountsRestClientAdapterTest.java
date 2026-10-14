package com.jcaa.usersmanagement.infrastructure.adapter.accounts;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.jcaa.usersmanagement.domain.enums.UserRole;
import com.jcaa.usersmanagement.domain.enums.UserStatus;
import com.jcaa.usersmanagement.domain.exception.AccountProvisioningException;
import com.jcaa.usersmanagement.domain.model.UserModel;
import com.jcaa.usersmanagement.domain.valueobject.UserEmail;
import com.jcaa.usersmanagement.domain.valueobject.UserId;
import com.jcaa.usersmanagement.domain.valueobject.UserName;
import com.jcaa.usersmanagement.domain.valueobject.UserPassword;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Tests para AccountsRestClientAdapter con un servidor HTTP simulado (mismo enfoque que el test del
 * cliente de Brevo).
 */
@DisplayName("AccountsRestClientAdapter")
class AccountsRestClientAdapterTest {

  private static final String BASE_URL = "https://accounts.test";
  private static final String URL = BASE_URL + "/api/accounts";
  private static final String API_KEY = "test-internal-api-key-0123456789";

  private RestClient.Builder restClientBuilder;
  private MockRestServiceServer server;

  @BeforeEach
  void setUp() {
    restClientBuilder =
        RestClient.builder()
            .baseUrl(BASE_URL)
            .defaultHeader(AccountsRestClientAdapter.API_KEY_HEADER, API_KEY)
            .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE);
    server = MockRestServiceServer.bindTo(restClientBuilder).build();
  }

  @Test
  @DisplayName("envía el id, correo y nombre del usuario con la clave interna")
  void shouldSendExpectedRequest() {
    // Arrange
    server
        .expect(once(), requestTo(URL))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header(AccountsRestClientAdapter.API_KEY_HEADER, API_KEY))
        .andExpect(jsonPath("$.userId").value("u-001"))
        .andExpect(jsonPath("$.email").value("john@example.com"))
        .andExpect(jsonPath("$.name").value("John Arrieta"))
        .andRespond(withStatus(HttpStatus.CREATED));

    // Act & Assert
    assertDoesNotThrow(() -> adapter(config(3, 5)).createFor(user()));
    server.verify();
  }

  @Test
  @DisplayName("reintenta errores transitorios y termina creando la cuenta")
  void shouldRetryTransientFailures() {
    // Arrange
    server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
    server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
    server.expect(requestTo(URL)).andRespond(withSuccess());

    // Act & Assert
    assertDoesNotThrow(() -> adapter(config(3, 5)).createFor(user()));
    server.verify();
  }

  @Test
  @DisplayName("no reintenta un 401 (clave incorrecta) y lanza AccountProvisioningException")
  void shouldNotRetryPermanentErrors() {
    // Arrange
    server.expect(once(), requestTo(URL)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

    // Act
    final AccountProvisioningException exception =
        assertThrows(
            AccountProvisioningException.class, () -> adapter(config(3, 5)).createFor(user()));

    // Assert
    assertTrue(exception.getMessage().contains("HTTP 401"));
    server.verify();
  }

  @Test
  @DisplayName("abre el circuito después del umbral de fallos")
  void shouldOpenCircuitAfterFailureThreshold() {
    // Arrange
    server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
    server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
    final AccountsRestClientAdapter adapter = adapter(config(1, 2));

    // Act
    assertThrows(AccountProvisioningException.class, () -> adapter.createFor(user()));
    assertThrows(AccountProvisioningException.class, () -> adapter.createFor(user()));
    final AccountProvisioningException openCircuit =
        assertThrows(AccountProvisioningException.class, () -> adapter.createFor(user()));

    // Assert
    assertTrue(openCircuit.getMessage().contains("circuit breaker abierto"));
    server.verify();
  }

  private AccountsRestClientAdapter adapter(final AccountsClientConfig config) {
    return new AccountsRestClientAdapter(config, restClientBuilder.build());
  }

  private static AccountsClientConfig config(final int attempts, final int minimumCalls) {
    return new AccountsClientConfig(
        BASE_URL,
        API_KEY,
        Duration.ofMillis(100),
        Duration.ofMillis(100),
        attempts,
        Duration.ofMillis(1),
        2.0,
        50,
        minimumCalls,
        minimumCalls,
        Duration.ofSeconds(30));
  }

  private static UserModel user() {
    return new UserModel(
        new UserId("u-001"),
        new UserName("John Arrieta"),
        new UserEmail("john@example.com"),
        UserPassword.fromHash("$2a$12$hash"),
        UserRole.MEMBER,
        UserStatus.PENDING);
  }
}
