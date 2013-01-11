package com.github.mustachejava.codes;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Iteration;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.TemplateContext;

import java.io.Writer;

/**
 * Runs the enclosed template once if the value is falsey.
 */
public class NotIterableCode extends DefaultCode implements Iteration {

  public NotIterableCode(TemplateContext templateContext, DefaultMustacheFactory cf, Mustache mustache, String variable) {
    super(templateContext, cf.getObjectHandler(), mustache, variable, "^");
  }

  @Override
  public Writer execute(Writer writer, Object[] scopes) {
    return appendText(oh.falsey(this, writer, get(scopes), scopes));
  }

  public Writer next(Writer writer, Object object, Object[] scopes) {
    return run(writer, scopes);
  }
}
