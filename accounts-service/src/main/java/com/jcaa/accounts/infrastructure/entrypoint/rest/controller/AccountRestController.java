package com.jcaa.accounts.infrastructure.entrypoint.rest.controller;

import com.jcaa.accounts.application.port.in.CreateAccountUseCase;
import com.jcaa.accounts.application.port.in.GetAccountByUserIdUseCase;
import com.jcaa.accounts.application.service.dto.result.AccountCreationResult;
import com.jcaa.accounts.infrastructure.entrypoint.rest.dto.request.CreateAccountRestRequest;
import com.jcaa.accounts.infrastructure.entrypoint.rest.dto.response.AccountRestResponse;
import com.jcaa.accounts.infrastructure.entrypoint.rest.mapper.AccountRestMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API interna: solo la llama users-service (paso 5 del diagrama), con el header {@code
 * X-Internal-Api-Key}. Responde 201 si creó la cuenta y 200 si el usuario ya la tenía.
 */
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountRestController {

  private final CreateAccountUseCase createAccountUseCase;
  private final GetAccountByUserIdUseCase getAccountByUserIdUseCase;

  @PostMapping
  public ResponseEntity<AccountRestResponse> create(
      @Valid @RequestBody final CreateAccountRestRequest request) {
    final AccountCreationResult result =
        createAccountUseCase.execute(AccountRestMapper.toCreateCommand(request));
    final HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
    return ResponseEntity.status(status).body(AccountRestMapper.toResponse(result.account()));
  }

  @GetMapping("/{userId}")
  public AccountRestResponse getByUserId(@PathVariable final String userId) {
    return AccountRestMapper.toResponse(
        getAccountByUserIdUseCase.execute(AccountRestMapper.toGetByUserIdQuery(userId)));
  }
}
