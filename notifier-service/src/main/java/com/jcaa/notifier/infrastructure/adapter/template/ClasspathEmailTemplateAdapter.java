package com.jcaa.notifier.infrastructure.adapter.template;

import com.jcaa.notifier.application.port.out.EmailTemplatePort;
import com.jcaa.notifier.domain.exception.EmailSenderException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

/**
 * Plantillas HTML en {@code src/main/resources/templates}. Los valores se escapan como HTML porque
 * vienen de datos del usuario (por ejemplo, su nombre).
 */
@Component
public class ClasspathEmailTemplateAdapter implements EmailTemplatePort {

  private static final String TEMPLATES_PATH = "/templates/";
  private static final String TOKEN_PREFIX = "{{";
  private static final String TOKEN_SUFFIX = "}}";

  @Override
  public String render(final String templateName, final Map<String, String> values) {
    String result = loadTemplate(templateName);
    for (final Map.Entry<String, String> entry : values.entrySet()) {
      final String token = TOKEN_PREFIX + entry.getKey() + TOKEN_SUFFIX;
      result = result.replace(token, HtmlUtils.htmlEscape(entry.getValue()));
    }
    return result;
  }

  private String loadTemplate(final String templateName) {
    try (InputStream inputStream = openResourceStream(TEMPLATES_PATH + templateName)) {
      if (Objects.isNull(inputStream)) {
        throw EmailSenderException.becauseTemplateWasNotFound(templateName);
      }
      return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (final IOException ioException) {
      throw EmailSenderException.becauseSendFailed(ioException);
    }
  }

  InputStream openResourceStream(final String path) {
    return getClass().getResourceAsStream(path);
  }
}
