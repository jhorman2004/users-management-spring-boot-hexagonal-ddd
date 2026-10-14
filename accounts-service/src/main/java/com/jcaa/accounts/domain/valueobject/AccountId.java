package com.jcaa.accounts.domain.valueobject;

import com.jcaa.accounts.domain.exception.InvalidAccountDataException;
import java.util.Objects;
import java.util.UUID;

public record AccountId(String value) {

  private static final String FIELD_NAME = "id";
  private static final int MAX_LENGTH = 36;

  public AccountId {
    if (Objects.isNull(value) || value.isBlank()) {
      throw InvalidAccountDataException.becauseValueIsBlank(FIELD_NAME);
    }
    value = value.trim();
    if (value.length() > MAX_LENGTH) {
      throw InvalidAccountDataException.becauseValueIsTooLong(FIELD_NAME, MAX_LENGTH);
    }
  }

  public static AccountId random() {
    return new AccountId(UUID.randomUUID().toString());
  }
}
