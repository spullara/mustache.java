package com.github.mustachejava.impl;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.concurrent.Callable;

import com.google.common.base.Function;
import com.google.common.util.concurrent.ListeningExecutorService;

import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheCompiler;
import com.github.mustachejava.MustacheException;
import com.github.mustachejava.util.LatchedWriter;

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
  private ListeningExecutorService les;

  public ValueCode(DefaultCodeFactory cf, String variable, String sm, String em, boolean encoded, int line) {
    super(cf.getObjectHandler(), null, variable, "", sm, em);
    this.cf = cf;
    this.variable = variable;
    this.encoded = encoded;
    this.line = line;
    les = cf.getListeningExecutorService();
  }

  @Override
  public Writer execute(Writer writer, final Object... scopes) {
    final Object object = get(variable, scopes);
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
            writer = mustache.execute(sw, scopes);
            value = sw.toString();
          } else {
            value = "";
          }
          execute(writer, value, scopes);
        } else {
          if (object instanceof Callable) {
            final Callable callable = (Callable) object;
            if (les == null) {
              execute(writer, callable.call().toString(), scopes);
            } else {
              final LatchedWriter latchedWriter = new LatchedWriter(writer);
              final Writer finalWriter = writer;
              les.execute(new Runnable() {
                @Override
                public void run() {
                  try {
                    Object call = callable.call();
                    execute(finalWriter, call == null ? null : call.toString(), scopes);
                    latchedWriter.done();
                  } catch (Throwable e) {
                    latchedWriter.failed(e);
                  }
                }
              });
              return super.execute(latchedWriter, scopes);
            }
          } else {
            execute(writer, object.toString(), scopes);
          }
        }
      } catch (Exception e) {
        throw new MustacheException("Failed to get value for " + variable + " at line " + line, e);
      }
    }
    return super.execute(writer, scopes);
  }

  private void execute(Writer writer, String value, Object... scopes) throws IOException {
    if (encoded) {
      writer.write(cf.encode(value));
    } else {
      writer.write(value);
    }
  }
}
