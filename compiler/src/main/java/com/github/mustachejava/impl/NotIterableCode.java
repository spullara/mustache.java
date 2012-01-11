package com.github.mustachejava.impl;

import java.io.Writer;
import java.util.Iterator;
import java.util.List;

import com.github.mustachejava.Code;

/**
* Created by IntelliJ IDEA.
* User: spullara
* Date: 1/9/12
* Time: 2:58 PM
* To change this template use File | Settings | File Templates.
*/
public class NotIterableCode extends DefaultCode {
  private final String variable;

  public NotIterableCode(DefaultMustacheFactory cf, List<Code> codes, String variable, String sm, String em) {
    super(cf.getObjectHandler(), codes.toArray(new Code[0]), variable, "^", sm, em);
    this.variable = variable;
  }

  @Override
  public Writer execute(Writer writer, Object... scopes) {
    Object resolve = get(variable, scopes);
    if (resolve != null) {
      Iterator i = iterate(resolve);
      if (!i.hasNext()) {
        writer = runCodes(writer, scopes);
      }
    } else {
      writer = runCodes(writer, scopes);
    }
    return appendText(writer);
  }
}
