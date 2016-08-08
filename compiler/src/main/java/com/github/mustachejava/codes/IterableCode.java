package com.github.mustachejava.codes;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.FragmentKey;
import com.github.mustachejava.Iteration;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheException;
import com.github.mustachejava.TemplateContext;
import com.github.mustachejava.TemplateFunction;
import com.github.mustachejava.util.InternalArrayList;
import com.github.mustachejava.util.LatchedWriter;
import com.github.mustachejava.util.Node;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static com.github.mustachejava.util.NodeValue.list;

public class IterableCode extends DefaultCode implements Iteration {

  private final ExecutorService les;

  public IterableCode(TemplateContext tc, DefaultMustacheFactory df, Mustache mustache, String variable, String type) {
    super(tc, df, mustache, variable, type);
    les = df.getExecutorService();
  }

  public IterableCode(TemplateContext tc, DefaultMustacheFactory df, Mustache mustache, String variable) {
    this(tc, df, mustache, variable, "#");
  }

  @Override
  public Writer execute(Writer writer, final List<Object> scopes) {
    Object resolved = get(scopes);
    writer = handle(writer, resolved, scopes);
    appendText(writer);
    return writer;
  }

  protected Writer handle(Writer writer, Object resolved, List<Object> scopes) {
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

  protected Writer handleCallable(Writer writer, final Callable callable, final List<Object> scopes) {
    if (les == null) {
      try {
        writer = execute(writer, callable.call(), scopes);
      } catch (Exception e) {
        throw new MustacheException(e, tc);
      }
    } else {
      // Flush the current writer
      try {
        writer.flush();
      } catch (IOException e) {
        throw new MustacheException("Failed to flush writer", e, tc);
      }
      final Writer originalWriter = writer;
      final LatchedWriter latchedWriter = new LatchedWriter(writer);
      writer = latchedWriter;
      // Scopes must not cross thread boundaries as they
      // are thread locally reused
      final List<Object> newScopes = new InternalArrayList<>(scopes);
      les.execute(() -> {
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
      });
    }
    return writer;
  }

  @SuppressWarnings("unchecked")
  protected Writer handleFunction(Writer writer, Function function, List<Object> scopes) {
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
        throw new MustacheException("Failed to write function result", e, tc);
      }
    }
    return writer;
  }

  protected Writer writeTemplate(Writer writer, String templateText, List<Object> scopes) {
    return df.getFragment(new FragmentKey(tc, templateText)).execute(writer, scopes);
  }

  protected Writer execute(Writer writer, Object resolve, List<Object> scopes) {
    return oh.iterate(this, writer, resolve, scopes);
  }

  public Writer next(Writer writer, Object next, List<Object> scopes) {
    boolean added = addScope(scopes, next);
    writer = run(writer, scopes);
    if (added) scopes.remove(scopes.size() - 1);
    return writer;
  }

  @Override
  public Node invert(Node node, String text, AtomicInteger position) {
    int start = position.get();
    List<Node> nodes = new ArrayList<>();
    Node invert;
    while ((invert = mustache.invert(new Node(), text, position)) != null) {
      nodes.add(invert);
    }
    node.put(name, list(nodes));
    return matchAppended(node, text, position, start);
  }
}
