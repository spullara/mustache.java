package com.github.mustachejava.codes;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.TemplateContext;
import com.github.mustachejava.util.Node;

import java.io.Writer;
import java.util.concurrent.atomic.AtomicInteger;

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

  @Override
  public Node invert(Node node, String text, AtomicInteger position) {
    if (text.substring(position.get()).startsWith(appended)) {
      position.addAndGet(appended.length());
      return node;
    } else {
      return null;
    }
  }
}
