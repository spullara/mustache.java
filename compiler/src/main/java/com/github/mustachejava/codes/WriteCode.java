package com.github.mustachejava.codes;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.TemplateContext;

import java.io.Writer;

/**
 * Write template text.
 */
public class WriteCode extends DefaultCode {
  public WriteCode(TemplateContext tc, DefaultMustacheFactory df, String text) {
    super(tc, df, null, null, null);
    super.append(text);
  }

  @Override
  public void identity(Writer writer) {
    execute(writer, null);
  }
}
