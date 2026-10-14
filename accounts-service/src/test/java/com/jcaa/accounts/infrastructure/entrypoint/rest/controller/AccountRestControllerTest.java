package com.jcaa.accounts.infrastructure.entrypoint.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jcaa.accounts.application.port.in.CreateAccountUseCase;
import com.jcaa.accounts.application.port.in.GetAccountByUserIdUseCase;
import com.jcaa.accounts.application.service.dto.result.AccountCreationResult;
import com.jcaa.accounts.domain.exception.AccountNotFoundException;
import com.jcaa.accounts.domain.model.AccountModel;
import com.jcaa.accounts.domain.valueobject.AccountUserId;
import com.jcaa.accounts.infrastructure.config.InternalApiKeySpringConfig;
import com.jcaa.accounts.infrastructure.security.InternalApiKeyFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tests de AccountRestController con el filtro de clave interna real (la clave viene de
 * src/test/resources/application.properties).
 */
@WebMvcTest(
    controllers = AccountRestController.class,
    properties = "spring.main.web-application-type=servlet")
@Import(InternalApiKeySpringConfig.class)
@DisplayName("AccountRestController")
class AccountRestControllerTest {

  private static final String API_KEY = "test-internal-api-key-0123456789";
  private static final String USER_ID = "u-001";
  private static final String BODY =
      "{\"userId\":\"u-001\",\"email\":\"john@example.com\",\"name\":\"John Arrieta\"}";

  @Autowired private MockMvc mockMvc;

  @MockBean private CreateAccountUseCase createAccountUseCase;
  @MockBean private GetAccountByUserIdUseCase getAccountByUserIdUseCase;

  @Test
  @DisplayName("POST /api/accounts responde 401 sin la clave interna")
  void shouldRejectRequestWithoutKey() throws Exception {
    // Act & Assert
    mockMvc
        .perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON).content(BODY))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401));
    verifyNoInteractions(createAccountUseCase);
  }

  @Test
  @DisplayName("POST /api/accounts responde 401 con una clave incorrecta")
  void shouldRejectRequestWithWrongKey() throws Exception {
    // Act & Assert
    mockMvc
        .perform(
            post("/api/accounts")
                .header(InternalApiKeyFilter.HEADER_NAME, "otra-clave-cualquiera-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
        .andExpect(status().isUnauthorized());
    verifyNoInteractions(createAccountUseCase);
  }

  @Test
  @DisplayName("POST /api/accounts responde 201 cuando crea la cuenta")
  void shouldReturnCreatedForNewAccount() throws Exception {
    // Arrange
    final AccountModel account = AccountModel.openFor(new AccountUserId(USER_ID));
    when(createAccountUseCase.execute(any())).thenReturn(new AccountCreationResult(account, true));

    // Act & Assert
    mockMvc
        .perform(
            post("/api/accounts")
                .header(InternalApiKeyFilter.HEADER_NAME, API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.userId").value(USER_ID))
        .andExpect(jsonPath("$.status").value("ACTIVE"));
  }

  @Test
  @DisplayName("POST /api/accounts responde 200 cuando el usuario ya tenía cuenta")
  void shouldReturnOkForExistingAccount() throws Exception {
    // Arrange
    final AccountModel account = AccountModel.openFor(new AccountUserId(USER_ID));
    when(createAccountUseCase.execute(any())).thenReturn(new AccountCreationResult(account, false));

    // Act & Assert
    mockMvc
        .perform(
            post("/api/accounts")
                .header(InternalApiKeyFilter.HEADER_NAME, API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("POST /api/accounts responde 400 cuando el body es inválido")
  void shouldReturnBadRequestForInvalidBody() throws Exception {
    // Act & Assert
    mockMvc
        .perform(
            post("/api/accounts")
                .header(InternalApiKeyFilter.HEADER_NAME, API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":\"\",\"email\":\"no-es-email\",\"name\":\"\"}"))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(createAccountUseCase);
  }

  @Test
  @DisplayName("GET /api/accounts/{userId} responde 404 cuando no hay cuenta")
  void shouldReturnNotFound() throws Exception {
    // Arrange
    when(getAccountByUserIdUseCase.execute(any()))
        .thenThrow(AccountNotFoundException.becauseUserHasNoAccount("u-404"));

    // Act & Assert
    mockMvc
        .perform(get("/api/accounts/u-404").header(InternalApiKeyFilter.HEADER_NAME, API_KEY))
        .andExpect(status().isNotFound());
  }
}
