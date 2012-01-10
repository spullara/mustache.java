package com.github.mustachejava.impl;

import java.io.Writer;
import java.util.concurrent.Callable;

import com.google.common.util.concurrent.ListeningExecutorService;

import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheException;
import com.github.mustachejava.util.LatchedWriter;

/**
* Created by IntelliJ IDEA.
* User: spullara
* Date: 1/10/12
* Time: 2:21 PM
* To change this template use File | Settings | File Templates.
*/
public class PartialCode extends DefaultCode {
  private Mustache partial;
  private final String variable;
  private final String extension;
  private DefaultCodeFactory cf;
  private final ListeningExecutorService les;

  public PartialCode(DefaultCodeFactory cf, String variable, String sm, String em, String extension) {
    super(cf.oh, null, variable, ">", sm, em);
    this.cf = cf;
    this.variable = variable;
    this.extension = extension;
    les = cf.getListeningExecutorService();
  }

  @Override
  public Writer execute(Writer writer, final Object... scopes) {
    if (partial == null) {
      partial = cf.mc.compile(variable + extension);
    }
    Object object = get(variable, scopes);
    if (object instanceof Callable) {
      final Callable callable = (Callable) object;
      if (les == null) {
        try {
          object = callable.call();
        } catch (Exception e) {
          throw new MustacheException(e);
        }
      } else {
        final LatchedWriter latchedWriter = new LatchedWriter(writer);
        final Writer finalWriter = writer;
        les.execute(new Runnable() {
          @Override
          public void run() {
            try {
              execute(finalWriter, callable.call(), scopes);
              latchedWriter.done();
            } catch (Throwable e) {
              latchedWriter.failed(e);
            }
          }
        });
        return latchedWriter;
      }
    }
    return execute(writer, object, scopes);
  }

  private Writer execute(Writer writer, Object scope, Object[] scopes) {
    Object[] newscopes = new Object[scopes.length + 1];
    System.arraycopy(scopes, 0, newscopes, 0, scopes.length);
    newscopes[scopes.length] = scope;
    return appendText(partial.execute(writer, newscopes));
  }
}
