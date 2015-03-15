package com.github.mustachejava.codes;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.TemplateContext;

import java.io.Writer;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Runs the enclosed template once if the value is falsey.
 */
public class NotIterableCode extends IterableCode {

  public NotIterableCode(TemplateContext templateContext, DefaultMustacheFactory df, Mustache mustache, String variable) {
    super(templateContext, df, mustache, variable, "^");
  }

  @Override
  public Writer execute(Writer writer, final List<Object> scopes) {
    Object resolved = get(scopes);
    writer = handle(writer, resolved, scopes);
    appendText(writer);
    return writer;
  }

  protected Writer handle(Writer writer, Object resolved, List<Object> scopes) {
    if (resolved instanceof Callable) {
      writer = handleCallable(writer, (Callable) resolved, scopes);
    } else {
      writer = execute(writer, resolved, scopes);
    }
    return writer;
  }

  @Override
  protected Writer execute(Writer writer, Object resolve, List<Object> scopes) {
    return oh.falsey(this, writer, resolve, scopes);
  }

  @Override
  public Writer next(Writer writer, Object object, List<Object> scopes) {
    return run(writer, scopes);
  }
}
