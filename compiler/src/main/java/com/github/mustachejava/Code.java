package com.github.mustachejava;

import java.io.Writer;

/**
 * Code objects that are executed in order to evaluate the template
 */
public interface Code {
  void execute(Writer writer, Object... scopes);
  void identity(Writer writer);
  void append(String text);
}
