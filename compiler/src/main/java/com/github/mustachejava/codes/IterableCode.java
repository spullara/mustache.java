package com.github.mustachejava.codes;

import com.github.mustachejava.*;
import com.github.mustachejava.util.LatchedWriter;
import com.google.common.base.Function;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

public class IterableCode extends DefaultCode implements Iteration {

  private final ExecutorService les;

  public IterableCode(TemplateContext tc, DefaultMustacheFactory df, Mustache mustache, String variable) {
    super(tc, df, mustache, variable, "#");
    les = df.getExecutorService();
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
      // Flush the current writer
      try {
        writer.flush();
      } catch (IOException e) {
        throw new MustacheException("Failed to flush writer", e);
      }
      final Writer originalWriter = writer;
      final LatchedWriter latchedWriter = new LatchedWriter(writer);
      writer = latchedWriter;
      // Scopes must not cross thread boundaries as they
      // are thread locally reused
      final Object[] newScopes = scopes.clone();
      les.execute(new Runnable() {
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

  @SuppressWarnings("unchecked")
  protected Writer handleFunction(Writer writer, Function function, Object[] scopes) {
    StringWriter sw = new StringWriter();
    runIdentity(sw);
    if (function instanceof TemplateFunction) {
      Object newtemplate = function.apply(sw.toString());
      if (newtemplate != null) {
        String templateText = newtemplate.toString();
        writer = writeTemplate(writer, templateText, scopes);
      }
    } else {
      try {
        StringWriter capture = new StringWriter();
        writeTemplate(capture, sw.toString(), scopes).close();
        Object apply = function.apply(capture.toString());
        if (apply != null) {
          writer.write(apply.toString());
        }
      } catch (IOException e) {
        throw new MustacheException("Failed to write function result", e);
      }
    }
    return writer;
  }

  protected Writer writeTemplate(Writer writer, String templateText, Object[] scopes) {
    return df.getFragment(new FragmentKey(tc, templateText)).execute(writer, scopes);
  }

  protected Writer execute(Writer writer, Object resolve, Object[] scopes) {
    return oh.iterate(this, writer, resolve, scopes);
  }

  public Writer next(Writer writer, Object next, Object... scopes) {
    Object[] iteratorScopes = addScope(scopes, next);
    writer = run(writer, iteratorScopes);
    return writer;
  }
}
