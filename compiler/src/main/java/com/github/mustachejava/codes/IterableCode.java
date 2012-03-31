package com.github.mustachejava.codes;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Iteration;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheException;
import com.github.mustachejava.TemplateContext;
import com.github.mustachejava.TemplateFunction;
import com.github.mustachejava.util.LatchedWriter;
import com.google.common.base.Function;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

/**
 * Created by IntelliJ IDEA.
 * User: spullara
 * Date: 1/9/12
 * Time: 2:57 PM
 * To change this template use File | Settings | File Templates.
 */
public class IterableCode extends DefaultCode implements Iteration {

  private final String variable;
  private DefaultMustacheFactory cf;
  private ExecutorService les;

  public IterableCode(TemplateContext tc, DefaultMustacheFactory cf, Mustache mustache, String variable) {
    super(tc, cf.getObjectHandler(), mustache, variable, "#");
    this.cf = cf;
    this.variable = variable;
    les = cf.getExecutorService();
  }

  @Override
  public Writer execute(Writer writer, final Object[] scopes) {
    Object resolve = get(variable, scopes);
    if (resolve != null) {
      if (resolve instanceof Function) {
        writer = handleFunction(writer, (Function) resolve, scopes);
      } else if (resolve instanceof Callable) {
        writer = handleCallable(writer, (Callable) resolve, scopes);
      } else {
        writer = execute(writer, resolve, scopes);
      }
    }
    return appendText(writer);
  }

  protected Writer handleCallable(Writer writer, final Callable callable, final Object[] scopes) {
    if (les == null) {
      try {
        writer = execute(writer, callable.call(), scopes);
      } catch (Exception e) {
        throw new MustacheException(e);
      }
    } else {
      final Writer finalWriter = writer;
      final LatchedWriter latchedWriter = new LatchedWriter(writer);
      writer = latchedWriter;
      // Scopes must not cross thread boundaries as they
      // are thread locally reused
      final Object[] newScopes = scopes.clone();
      les.execute(new Runnable() {
        @Override
        public void run() {
          try {
            Object call = callable.call();
            execute(finalWriter, call, newScopes);
            latchedWriter.done();
          } catch (Throwable e) {
            latchedWriter.failed(e);
          }
        }
      });
    }
    return writer;
  }

  protected Writer handleFunction(Writer writer, Function function, Object[] scopes) {
    StringWriter sw = new StringWriter();
    runIdentity(sw);
    Object newtemplate = function.apply(sw.toString());
    if (newtemplate != null) {
      if (function instanceof TemplateFunction) {
        String templateText = newtemplate.toString();
        Mustache mustache = cf.getTemplate(templateText);
        if (mustache == null) {
          mustache = cf.compile(new StringReader(templateText), tc.file(), tc.startChars(), tc.endChars());
          cf.putTemplate(templateText, mustache);
        }
        writer = mustache.execute(writer, scopes);
      } else {
        try {
          writer.write(newtemplate.toString());
        } catch (IOException e) {
          throw new MustacheException("Failed to write function result", e);
        }
      }
    }
    return writer;
  }

  protected Writer execute(Writer writer, Object resolve, Object[] scopes) {
    return oh.iterate(this, writer, resolve, scopes);
  }

  public Writer next(Writer writer, Object next, Object... scopes) {
    Object[] iteratorScopes = addScope(next, scopes);
    writer = runCodes(writer, iteratorScopes);
    return writer;
  }

}
