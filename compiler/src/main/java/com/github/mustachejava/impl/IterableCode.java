package com.github.mustachejava.impl;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;

import com.google.common.base.Function;

import com.github.mustachejava.Code;
import com.github.mustachejava.Mustache;

/**
* Created by IntelliJ IDEA.
* User: spullara
* Date: 1/9/12
* Time: 2:57 PM
* To change this template use File | Settings | File Templates.
*/
public class IterableCode extends DefaultCode {

  private final String variable;
  private final String file;
  private DefaultCodeFactory cf;

  public IterableCode(DefaultCodeFactory cf, List<Code> codes, String variable, String sm, String em, String file) {
    super(cf.getObjectHandler(), codes.toArray(new Code[0]), variable, "#", sm, em);
    this.cf = cf;
    this.variable = variable;
    this.file = file;
  }

  @Override
  public Writer execute(Writer writer, Object... scopes) {
    Object resolve = get(variable, scopes);
    if (resolve != null) {
      if (resolve instanceof Function) {
        Function f = (Function) resolve;
        StringWriter sw = new StringWriter();
        runIdentity(sw);
        Object newtemplate = f.apply(sw.toString());
        if (newtemplate != null) {
          String templateText = newtemplate.toString();
          Mustache mustache = cf.templateCache.get(templateText);
          if (mustache == null) {
            mustache = cf.mc.compile(new StringReader(templateText), file, sm, em);
            cf.templateCache.put(templateText, mustache);
          }
          writer = mustache.execute(writer, scopes);
        }
      } else {
        for (Iterator i = oh.iterate(resolve); i.hasNext(); ) {
          Object next = i.next();
          Object[] iteratorScopes = scopes;
          if (next != null) {
            iteratorScopes = new Object[scopes.length + 1];
            System.arraycopy(scopes, 0, iteratorScopes, 0, scopes.length);
            iteratorScopes[scopes.length] = next;
          }
          writer = runCodes(writer, iteratorScopes);
        }
      }
    }
    return appendText(writer);
  }
}
