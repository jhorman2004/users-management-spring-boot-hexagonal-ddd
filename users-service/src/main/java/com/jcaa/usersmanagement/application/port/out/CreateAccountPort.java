package com.jcaa.usersmanagement.application.port.out;

import com.jcaa.usersmanagement.domain.model.UserModel;

/** Paso 5 del diagrama: crea (de forma síncrona) la cuenta del usuario en accounts-service. */
public interface CreateAccountPort {
  /**
   * @throws com.jcaa.usersmanagement.domain.exception.AccountProvisioningException si no se pudo
   *     crear la cuenta después de los reintentos
   */
  void createFor(UserModel user);
}
