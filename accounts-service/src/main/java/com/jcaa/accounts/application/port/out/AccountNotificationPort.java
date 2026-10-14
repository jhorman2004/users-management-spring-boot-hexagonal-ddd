package com.jcaa.accounts.application.port.out;

import com.jcaa.accounts.domain.model.AccountModel;

/** Paso 7 del diagrama: avisa (sin esperar) que hay que mandar el correo de "cuenta creada". */
public interface AccountNotificationPort {
  void notifyAccountCreated(AccountModel account, String recipientEmail, String recipientName);
}
