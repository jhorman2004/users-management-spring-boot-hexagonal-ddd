package com.jcaa.notifier.application.service.dto.command;

import com.jcaa.events.notification.NotificationType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record SendEmailNotificationCommand(
    @NotBlank(message = "eventId must not be blank") String eventId,
    @NotNull(message = "type must not be null") NotificationType type,
    @NotBlank(message = "recipientEmail must not be blank")
        @Email(message = "recipientEmail must be a valid email address")
        String recipientEmail,
    @NotBlank(message = "recipientName must not be blank") String recipientName,
    @NotNull(message = "data must not be null") Map<String, String> data) {}
