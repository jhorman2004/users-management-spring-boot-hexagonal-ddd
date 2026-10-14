package com.jcaa.usersmanagement.application.port.out;

import com.jcaa.usersmanagement.domain.model.UserModel;

/**
 * Avisa que hay que mandarle un correo al usuario. La implementación publica el mensaje y vuelve de
 * inmediato: el correo lo envía otro servicio de forma asíncrona.
 */
public interface UserNotificationPort {
  void notifyUserCreated(UserModel user);

  void notifyUserUpdated(UserModel user);
}
