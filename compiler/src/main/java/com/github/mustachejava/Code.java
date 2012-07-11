package com.github.mustachejava;

import java.io.Writer;

/**
 * Code objects that are executed in order to evaluate the template
 */
public interface Code {
  Writer execute(Writer writer, Object scope);

  Writer execute(Writer writer, Object[] scopes);

  void identity(Writer writer);

  void append(String text);

  Code[] getCodes();

  void setCodes(Code[] codes);

  void init();

  Object clone();
}
