package com.github.mustachejava.impl;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

import com.google.common.base.Function;

import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheCompiler;
import com.github.mustachejava.MustacheException;

/**
* Created by IntelliJ IDEA.
* User: spullara
* Date: 1/9/12
* Time: 2:58 PM
* To change this template use File | Settings | File Templates.
*/
public class ValueCode extends DefaultCode {
  private final String variable;
  private final boolean encoded;
  private final int line;
  private DefaultCodeFactory cf;

  public ValueCode(DefaultCodeFactory cf, String variable, String sm, String em, boolean encoded, int line) {
    super(null, variable, "", sm, em);
    this.cf = cf;
    this.variable = variable;
    this.encoded = encoded;
    this.line = line;
  }

  @Override
  public void execute(Writer writer, Object... scopes) {
    Object object = cf.resolve(variable, scopes);
    if (object != null) {
      try {
        String value;
        if (object instanceof Function) {
          Function f = (Function) object;
          Object newtemplate = f.apply(null);
          if (newtemplate != null) {
            String templateText = newtemplate.toString();
            Mustache mustache = cf.templateCache.get(templateText);
            if (mustache == null) {
              mustache = cf.mc.compile(new StringReader(templateText), variable,
                      MustacheCompiler.DEFAULT_SM, MustacheCompiler.DEFAULT_EM);
              cf.templateCache.put(templateText, mustache);
            }
            StringWriter sw = new StringWriter();
            mustache.execute(sw, scopes);
            value = sw.toString();
          } else {
            value = "";
          }
        } else {
          value = object.toString();
        }
        if (encoded) {
          writer.write(cf.encode(value));
        } else {
          writer.write(value);
        }
      } catch (Exception e) {
        throw new MustacheException("Failed to get value for " + variable + " at line " + line, e);
      }
    }
    super.execute(writer, scopes);
  }
}
