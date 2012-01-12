package com.github.mustachejava.codes;

import java.io.Writer;
import java.util.Iterator;
import java.util.List;

import com.github.mustachejava.Code;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Iteration;

/**
* Created by IntelliJ IDEA.
* User: spullara
* Date: 1/9/12
* Time: 2:58 PM
* To change this template use File | Settings | File Templates.
*/
public class NotIterableCode extends DefaultCode implements Iteration {
  private final String variable;

  public NotIterableCode(DefaultMustacheFactory cf, List<Code> codes, String variable, String sm, String em) {
    super(cf.getObjectHandler(), codes.toArray(new Code[0]), variable, "^", sm, em);
    this.variable = variable;
  }

  @Override
  public Writer execute(Writer writer, Object... scopes) {
    return appendText(oh.falsey(this, writer, get(variable, scopes), scopes));
  }

  @Override
  public Writer next(Writer writer, Object object, Object[] scopes) {
    return runCodes(writer, scopes);
  }
}
