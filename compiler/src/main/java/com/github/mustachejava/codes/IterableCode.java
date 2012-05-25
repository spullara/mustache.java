package com.github.mustachejava.codes;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import com.google.common.base.Function;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Iteration;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheException;
import com.github.mustachejava.TemplateContext;
import com.github.mustachejava.TemplateFunction;
import com.github.mustachejava.util.LatchedWriter;

/**
 * Created by IntelliJ IDEA.
 * User: spullara
 * Date: 1/9/12
 * Time: 2:57 PM
 * To change this template use File | Settings | File Templates.
 */
public class IterableCode extends DefaultCode implements Iteration {

  private DefaultMustacheFactory cf;
  private ExecutorService les;

  public IterableCode(TemplateContext tc, DefaultMustacheFactory cf, Mustache mustache, String variable) {
    super(tc, cf.getObjectHandler(), mustache, variable, "#");
    this.cf = cf;
    les = cf.getExecutorService();
  }

  @Override
  public Writer execute(Writer writer, final Object[] scopes) {
    Object resolved = get(scopes);
    writer = handle(writer, resolved, scopes);
    appendText(writer);
    return writer;
  }

  private Writer handle(Writer writer, Object resolved, Object[] scopes) {
    if (resolved != null) {
      if (resolved instanceof Function) {
        writer = handleFunction(writer, (Function) resolved, scopes);
      } else if (resolved instanceof Callable) {
        writer = handleCallable(writer, (Callable) resolved, scopes);
      } else {
        writer = execute(writer, resolved, scopes);
      }
    }
    return writer;
  }

  protected Writer handleCallable(Writer writer, final Callable callable, final Object[] scopes) {
    if (les == null) {
      try {
        writer = execute(writer, callable.call(), scopes);
      } catch (Exception e) {
        throw new MustacheException(e);
      }
    } else {
      final Writer originalWriter = writer;
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
            Writer subWriter = handle(originalWriter, call, newScopes);
            // Wait for the subwriter to complete
            if (subWriter instanceof LatchedWriter) {
              ((LatchedWriter) subWriter).await();
            }
            // Tell the replacement writer that we are done
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
        writer = writeTemplate(writer, templateText, scopes);
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

  protected Writer writeTemplate(Writer writer, String templateText, Object[] scopes) {
    Mustache mustache = cf.getTemplate(templateText);
    if (mustache == null) {
      mustache = cf.compile(new StringReader(templateText), tc.file(), tc.startChars(), tc.endChars());
      cf.putTemplate(templateText, mustache);
    }
    writer = mustache.execute(writer, scopes);
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
