package com.jcaa.accounts.domain.exception;

public abstract class DomainException extends RuntimeException {

  protected DomainException(final String message) {
    super(message);
  }
}
