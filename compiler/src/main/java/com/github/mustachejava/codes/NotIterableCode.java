package com.github.mustachejava.codes;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Iteration;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.TemplateContext;

import java.io.Writer;

/**
 * Created by IntelliJ IDEA.
 * User: spullara
 * Date: 1/9/12
 * Time: 2:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class NotIterableCode extends DefaultCode implements Iteration {

  public NotIterableCode(TemplateContext templateContext, DefaultMustacheFactory cf, Mustache mustache, String variable) {
    super(templateContext, cf.getObjectHandler(), mustache, variable, "^");
  }

  @Override
  public Writer execute(Writer writer, Object[] scopes) {
    return appendText(oh.falsey(this, writer, get(scopes), scopes));
  }

  @Override
  public Writer next(Writer writer, Object object, Object[] scopes) {
    return runCodes(writer, scopes);
  }
}
