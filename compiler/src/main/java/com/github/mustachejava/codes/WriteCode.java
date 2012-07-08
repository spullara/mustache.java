package com.github.mustachejava.codes;

import java.io.Writer;

/**
 * Write template text.
 */
public class WriteCode extends DefaultCode {
  public WriteCode(String text) {
    super.append(text);
  }

  @Override
  public void identity(Writer writer) {
    execute(writer, null);
  }
}
