package com.github.mustachejava.codes;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.TemplateContext;
import com.github.mustachejava.util.Node;

import java.io.Writer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

  private Pattern compiledAppended;

  @Override
  public Node invert(Node node, String text, AtomicInteger position) {
    if (compiledAppended == null) {
      compiledAppended = Pattern.compile(appended);
    }
    Matcher matcher = compiledAppended.matcher(text);
    if (matcher.find(position.get())) {
      position.set(matcher.start() + matcher.group(0).length());
      return node;
    } else {
      return null;
    }
  }
}
