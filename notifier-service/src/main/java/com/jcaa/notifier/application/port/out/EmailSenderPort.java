package com.jcaa.notifier.application.port.out;

import com.jcaa.notifier.domain.model.EmailDestinationModel;

public interface EmailSenderPort {
  void send(EmailDestinationModel destination);
}
