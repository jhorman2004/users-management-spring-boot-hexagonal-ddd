package com.jcaa.accounts.domain.valueobject;

import com.jcaa.accounts.domain.exception.InvalidAccountDataException;
import java.util.Objects;

/** ID del usuario dueño de la cuenta (viene de users-service; mismo límite de 36 caracteres). */
public record AccountUserId(String value) {

  private static final String FIELD_NAME = "userId";
  private static final int MAX_LENGTH = 36;

  public AccountUserId {
    if (Objects.isNull(value) || value.isBlank()) {
      throw InvalidAccountDataException.becauseValueIsBlank(FIELD_NAME);
    }
    value = value.trim();
    if (value.length() > MAX_LENGTH) {
      throw InvalidAccountDataException.becauseValueIsTooLong(FIELD_NAME, MAX_LENGTH);
    }
  }
}
