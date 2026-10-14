package com.jcaa.accounts.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcaa.accounts.infrastructure.entrypoint.rest.dto.response.ApiErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Solo deja pasar las peticiones que traen la clave compartida con users-service en el header
 * {@value #HEADER_NAME}. La comparación es de tiempo constante para no filtrar la clave por tiempos.
 */
@Slf4j
public class InternalApiKeyFilter extends OncePerRequestFilter {

  public static final String HEADER_NAME = "X-Internal-Api-Key";

  private static final String MESSAGE_UNAUTHORIZED = "Clave interna ausente o inválida.";
  private static final String LOG_REJECTED =
      "[InternalApiKeyFilter] petición rechazada por clave interna inválida. path={}";

  private final byte[] expectedKey;
  private final ObjectMapper objectMapper;

  public InternalApiKeyFilter(final String expectedKey, final ObjectMapper objectMapper) {
    this.expectedKey = expectedKey.getBytes(StandardCharsets.UTF_8);
    this.objectMapper = objectMapper;
  }

  @Override
  protected void doFilterInternal(
      final HttpServletRequest request,
      final HttpServletResponse response,
      final FilterChain filterChain)
      throws ServletException, IOException {
    final String providedKey = request.getHeader(HEADER_NAME);
    if (Objects.isNull(providedKey)
        || !MessageDigest.isEqual(expectedKey, providedKey.getBytes(StandardCharsets.UTF_8))) {
      log.warn(LOG_REJECTED, request.getRequestURI());
      writeUnauthorized(response);
      return;
    }
    filterChain.doFilter(request, response);
  }

  private void writeUnauthorized(final HttpServletResponse response) throws IOException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    objectMapper.writeValue(
        response.getOutputStream(),
        new ApiErrorResponse(HttpServletResponse.SC_UNAUTHORIZED, MESSAGE_UNAUTHORIZED));
  }
}
