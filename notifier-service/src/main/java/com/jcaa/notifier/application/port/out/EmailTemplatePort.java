package com.jcaa.notifier.application.port.out;

import java.util.Map;

public interface EmailTemplatePort {
  /** Carga la plantilla y reemplaza cada {@code {{clave}}} por su valor. */
  String render(String templateName, Map<String, String> values);
}
