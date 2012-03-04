package com.sampullara.mustache.code;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

import com.sampullara.mustache.Code;
import com.sampullara.mustache.Mustache;
import com.sampullara.mustache.MustacheException;
import com.sampullara.mustache.Scope;
import com.sampullara.util.FutureWriter;

/**
* This handles explicit functions.
* <p/>
* User: sam
* Date: 11/27/11
* Time: 10:36 AM
*/
public class FunctionCode extends SubCode {
  public FunctionCode(Mustache m, String variable, List<Code> codes, String file, int line) {
    super("_", m, variable, codes, file, line);
  }

  @Override
  public void execute(FutureWriter fw, Scope scope) throws MustacheException {
    Object function = m.getValue(scope, variable);
    if (function instanceof Function) {
      execute(fw, m.function(scope, (Function) function));
    } else if (function == null) {
      execute(fw, Lists.newArrayList(scope));
    } else {
      throw new MustacheException("Not a function: " + function);
    }
  }

  @Override
  public Scope unexecute(Scope current, final String text, final AtomicInteger position, Code[] next) throws MustacheException {
    String value = new WriteValueCode(m, variable, false, position.get()).unexecuteValueCode(current, text, position, next);
    if (value == null) return null;
    Scope function = (Scope) current.get(variable);
    if (function == null) {
      function = new UnexecuteFunction();
      BuilderCodeFactory.put(current, variable, function);
    }
    StringWriter sw = new StringWriter();
    FutureWriter fw = new FutureWriter(sw);
    try {
      for (Code code : codes) {
        code.execute(fw, current);
      }
      fw.flush();
    } catch (IOException e) {
      throw new MustacheException("Failed to evaluate function body", e);
    }
    function.put(sw.toString(), value);
    return current;
  }

  private static class UnexecuteFunction extends Scope implements Function<String, String> {
    @Override
    public String apply(String input) {
      Object o = get(input);
      return o == null ? "" : o.toString();
    }
  }

}
