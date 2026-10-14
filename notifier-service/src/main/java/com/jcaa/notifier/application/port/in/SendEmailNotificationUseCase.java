package com.jcaa.notifier.application.port.in;

import com.jcaa.notifier.application.service.dto.command.SendEmailNotificationCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface SendEmailNotificationUseCase {
  void execute(@NotNull @Valid SendEmailNotificationCommand command);
}
