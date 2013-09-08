package com.github.mustachejava.codes;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.FragmentKey;
import com.github.mustachejava.MustacheException;
import com.github.mustachejava.TemplateContext;
import com.github.mustachejava.util.LatchedWriter;
import com.github.mustachejava.util.Node;
import com.google.common.base.Function;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.github.mustachejava.MustacheParser.DEFAULT_EM;
import static com.github.mustachejava.MustacheParser.DEFAULT_SM;
import static com.github.mustachejava.util.NodeValue.value;

/**
 * Output a value
 */
public class ValueCode extends DefaultCode {
  private final boolean encoded;
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

  public ValueCode(TemplateContext tc, DefaultMustacheFactory df, String variable, boolean encoded) {
    super(tc, df, null, variable, "");
    this.encoded = encoded;
    les = df.getExecutorService();
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
      Object call = callable.call();
      execute(writer, call == null ? null : oh.stringify(call));
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
    if (newtemplate == null) {
      value = "";
    } else {
      String templateText = newtemplate.toString();
      StringWriter sw = new StringWriter();
      TemplateContext newTC = new TemplateContext(DEFAULT_SM, DEFAULT_EM, tc.file(), tc.line(), tc.startOfLine());
      df.getFragment(new FragmentKey(newTC, templateText)).execute(sw, scopes).close();
      value = sw.toString();
    }
    execute(writer, value);
  }

  protected void execute(Writer writer, String value) throws IOException {
    // Treat null values as the empty string
    if (value != null) {
      if (encoded) {
        df.encode(value, writer);
      } else {
        writer.write(value);
      }
    }
  }

  private Pattern compiledAppended;

  @Override
  public Node invert(Node node, String text, AtomicInteger position) {
    if (compiledAppended == null) {
      compiledAppended = Pattern.compile(appended);
    }
    int start = position.get();
    Matcher matcher = compiledAppended.matcher(text);
    if (matcher.find(position.get())) {
      String value = text.substring(start, matcher.start());
      position.set(matcher.start() + matcher.group(0).length());
      node.put(name, value(value));
      return node;
    } else {
      return null;
    }
  }
}
