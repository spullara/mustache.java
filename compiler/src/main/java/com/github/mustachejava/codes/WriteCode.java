package com.github.mustachejava.codes;

import com.github.mustachejava.DefaultMustacheFactory;

import java.io.Writer;

/**
 * Write template text.
 */
public class WriteCode extends DefaultCode {
  public WriteCode(DefaultMustacheFactory df, String text) {
    super.append(text);
  }

  @Override
  public void identity(Writer writer) {
    execute(writer, null);
  }
}
