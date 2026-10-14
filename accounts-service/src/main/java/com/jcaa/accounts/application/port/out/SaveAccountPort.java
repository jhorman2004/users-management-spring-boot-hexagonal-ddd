package com.jcaa.accounts.application.port.out;

import com.jcaa.accounts.domain.model.AccountModel;

public interface SaveAccountPort {
  /**
   * Guarda una cuenta nueva.
   *
   * @throws com.jcaa.accounts.domain.exception.AccountAlreadyExistsException si el usuario ya
   *     tiene cuenta
   */
  AccountModel save(AccountModel account);
}
