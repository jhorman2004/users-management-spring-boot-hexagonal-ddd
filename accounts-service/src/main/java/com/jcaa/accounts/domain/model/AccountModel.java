package com.jcaa.accounts.domain.model;

import com.jcaa.accounts.domain.enums.AccountStatus;
import com.jcaa.accounts.domain.valueobject.AccountId;
import com.jcaa.accounts.domain.valueobject.AccountUserId;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.Value;

/**
 * Cuenta de un usuario. Cada usuario tiene como máximo una cuenta. Solo guarda identificadores: el
 * nombre y el correo del usuario viven en users-service.
 */
@Value
public class AccountModel {

  AccountId id;
  AccountUserId userId;
  AccountStatus status;
  Instant createdAt;

  /** Abre una cuenta nueva y activa para el usuario (precisión de milisegundos, como la BD). */
  public static AccountModel openFor(final AccountUserId userId) {
    return new AccountModel(
        AccountId.random(),
        userId,
        AccountStatus.ACTIVE,
        Instant.now().truncatedTo(ChronoUnit.MILLIS));
  }
}
