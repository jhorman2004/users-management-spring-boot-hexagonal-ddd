package com.jcaa.accounts.infrastructure.entrypoint.rest.dto.response;

import java.time.Instant;

public record AccountRestResponse(String id, String userId, String status, Instant createdAt) {}
