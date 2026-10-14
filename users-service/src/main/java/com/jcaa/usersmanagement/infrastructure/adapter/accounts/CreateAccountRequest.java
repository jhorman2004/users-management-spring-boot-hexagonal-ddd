package com.jcaa.usersmanagement.infrastructure.adapter.accounts;

/** Cuerpo de {@code POST /api/accounts} en accounts-service. */
record CreateAccountRequest(String userId, String email, String name) {}
