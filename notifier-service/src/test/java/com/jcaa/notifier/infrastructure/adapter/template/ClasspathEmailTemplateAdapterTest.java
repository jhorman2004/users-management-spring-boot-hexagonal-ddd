package com.jcaa.notifier.infrastructure.adapter.template;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import com.jcaa.notifier.domain.exception.EmailSenderException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests para ClasspathEmailTemplateAdapter.
 *
 * <p>Usa las plantillas reales del classpath y, para las ramas de error, sobrescribe {@code
 * openResourceStream}.
 */
@DisplayName("ClasspathEmailTemplateAdapter")
class ClasspathEmailTemplateAdapterTest {

  private final ClasspathEmailTemplateAdapter adapter = new ClasspathEmailTemplateAdapter();

  @Test
  @DisplayName("render() reemplaza los tokens de la plantilla de bienvenida")
  void shouldReplaceTokens() {
    // Act
    final String html =
        adapter.render(
            "user-created.html",
            Map.of("name", "John", "email", "john@example.com", "role", "ADMIN", "status", "PENDING"));

    // Assert
    assertAll(
        () -> assertTrue(html.contains("Hola, John")),
        () -> assertTrue(html.contains("john@example.com")),
        () -> assertTrue(html.contains("ADMIN")),
        () -> assertFalse(html.contains("{{name}}")));
  }

  @Test
  @DisplayName("render() escapa el HTML de los valores")
  void shouldEscapeHtmlInValues() {
    // Act
    final String html =
        adapter.render("account-created.html", Map.of("name", "<script>x</script>"));

    // Assert
    assertAll(
        () -> assertFalse(html.contains("<script>")),
        () -> assertTrue(html.contains("&lt;script&gt;")));
  }

  @Test
  @DisplayName("la plantilla de bienvenida ya no muestra la contraseña")
  void shouldNotIncludePasswordPlaceholder() {
    // Act
    final String html = adapter.render("user-created.html", Map.of());

    // Assert
    assertFalse(html.contains("{{password}}"));
  }

  @Test
  @DisplayName("render() lanza EmailSenderException cuando la plantilla no existe")
  void shouldThrowWhenTemplateIsMissing() {
    // Act & Assert
    assertThrows(EmailSenderException.class, () -> adapter.render("missing.html", Map.of()));
  }

  @Test
  @DisplayName("render() lanza EmailSenderException cuando falla la lectura")
  void shouldThrowWhenTemplateCannotBeRead() throws IOException {
    // Arrange
    try (InputStream brokenStream = mock(InputStream.class)) {
      doThrow(new IOException("Disk error")).when(brokenStream).readAllBytes();
      final ClasspathEmailTemplateAdapter brokenAdapter =
          new ClasspathEmailTemplateAdapter() {
            @Override
            InputStream openResourceStream(final String path) {
              return brokenStream;
            }
          };

      // Act & Assert
      assertThrows(
          EmailSenderException.class, () -> brokenAdapter.render("user-created.html", Map.of()));
    }
  }
}
