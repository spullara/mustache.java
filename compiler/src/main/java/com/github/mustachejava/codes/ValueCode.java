package com.github.mustachejava.codes;

import com.github.mustachejava.*;
import com.github.mustachejava.util.LatchedWriter;
import com.google.common.base.Function;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

/**
 * Output a value
 */
public class ValueCode extends DefaultCode {
  private final boolean encoded;
  private final DefaultMustacheFactory cf;
  private final ExecutorService les;

  @Override
  public void identity(Writer writer) {
    try {
      if (name != null) {
        writer.write(tc.startChars());
        if (!encoded) {
          writer.write("{");
        }
        writer.write(type);
        writer.write(name);
        if (!encoded) {
          writer.write("}");
        }
        writer.write(tc.endChars());
      }
      appendText(writer);
    } catch (IOException e) {
      throw new MustacheException(e);
    }
  }

  public ValueCode(TemplateContext tc, DefaultMustacheFactory cf, String variable, boolean encoded) {
    super(tc, cf.getObjectHandler(), null, variable, "");
    this.cf = cf;
    this.encoded = encoded;
    les = cf.getExecutorService();
  }

  @Override
  public Writer execute(Writer writer, final Object[] scopes) {
    final Object object = get(scopes);
    if (object != null) {
      try {
        if (object instanceof Function) {
          handleFunction(writer, (Function) object, scopes);
        } else if (object instanceof Callable) {
          return handleCallable(writer, (Callable) object, scopes);
        } else {
          execute(writer, oh.stringify(object));
        }
      } catch (Exception e) {
        throw new MustacheException("Failed to get value for " + name + " at line " + tc.file() + ":" + tc.line(), e);
      }
    }
    return super.execute(writer, scopes);
  }

  protected Writer handleCallable(Writer writer, final Callable callable, final Object[] scopes) throws Exception {
    if (les == null) {
      execute(writer, oh.stringify(callable.call()));
      return super.execute(writer, scopes);
    } else {
      // Flush the current writer
      try {
        writer.flush();
      } catch (IOException e) {
        throw new MustacheException("Failed to flush writer", e);
      }
      final LatchedWriter latchedWriter = new LatchedWriter(writer);
      final Writer finalWriter = writer;
      les.execute(new Runnable() {
        @Override
        public void run() {
          try {
            Object call = callable.call();
            execute(finalWriter, call == null ? null : oh.stringify(call));
            latchedWriter.done();
          } catch (Throwable e) {
            latchedWriter.failed(e);
          }
        }
      });
      return super.execute(latchedWriter, scopes);
    }
  }

  @SuppressWarnings("unchecked")
  protected void handleFunction(Writer writer, Function function, Object[] scopes) throws IOException {
    String value;
    Object newtemplate = function.apply(null);
    if (newtemplate != null) {
      String templateText = newtemplate.toString();
      Mustache mustache = cf.getTemplate(templateText);
      if (mustache == null) {
        mustache = cf.compile(new StringReader(templateText), name,
                MustacheParser.DEFAULT_SM, MustacheParser.DEFAULT_EM);
        cf.putTemplate(templateText, mustache);
      }
      StringWriter sw = new StringWriter();
      mustache.execute(sw, scopes);
      value = sw.toString();
    } else {
      value = "";
    }
    execute(writer, value);
  }

  protected void execute(Writer writer, String value) throws IOException {
    if (encoded) {
      cf.encode(value, writer);
    } else {
      writer.write(value);
    }
  }
  
}
