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

  public NotIterableCode(DefaultCodeFactory cf, List<Code> codes, String variable, String sm, String em) {
    super(cf.getObjectHandler(), codes.toArray(new Code[0]), variable, "^", sm, em);
    this.variable = variable;
  }

  @Override
  public void execute(Writer writer, Object... scopes) {
    Object resolve = resolve(variable, scopes);
    if (resolve != null) {
      Iterator i = oh.iterate(resolve);
      if (!i.hasNext()) {
        runCodes(writer, scopes);
      }
    } else {
      runCodes(writer, scopes);
    }
    appendText(writer);
  }
}
