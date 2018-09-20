package com.github.mustachejava;

import com.github.mustachejava.codes.PartialCode;
import com.github.mustachejava.util.GuardException;
import com.github.mustachejava.util.InternalArrayList;
import com.github.mustachejava.util.Wrapper;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This allows you to automatically defer evaluation of partials. By default
 * it generates HTML but you can override that behavior.
 */
public class DeferringMustacheFactory extends DefaultMustacheFactory {

  public static final Object DEFERRED = new Object();

  public DeferringMustacheFactory() {
  }

  public DeferringMustacheFactory(String resourceRoot) {
    super(resourceRoot);
  }

  public DeferringMustacheFactory(File fileRoot) {
    super(fileRoot);
  }

  private static class Deferral {
    final long id;
    final Future<Object> future;

    Deferral(long id, Future<Object> future) {
      this.id = id;
      this.future = future;
    }
  }

  public static class DeferredCallable implements Callable<String> {

    private List<Deferral> deferrals = new ArrayList<>();

    public void add(Deferral deferral) {
      deferrals.add(deferral);
    }

    @Override
    public String call() throws Exception {
      StringBuilder sb = new StringBuilder();
      for (Deferral deferral : deferrals) {
        Object o = deferral.future.get();
        if (o != null) {
          writeDeferral(sb, deferral, o);
        }
      }
      return sb.toString();
    }
  }

  @Override
  public MustacheVisitor createMustacheVisitor() {
    final AtomicLong id = new AtomicLong(0);
    return new DefaultMustacheVisitor(this) {
      @Override
      public void partial(TemplateContext tc, final String variable, final String indent) {
        TemplateContext partialTC = new TemplateContext("{{", "}}", tc.file(), tc.line(), tc.startOfLine());
        final Long divid = id.incrementAndGet();
        list.add(new PartialCode(partialTC, df, variable, indent) {
          Wrapper deferredWrapper;

          @Override
          public Writer execute(Writer writer, final List<Object> scopes) {

            final Object object = get(scopes);
            final DeferredCallable deferredCallable = getDeferred(scopes);
            if (object == DEFERRED && deferredCallable != null) {
              try {
                writeTarget(writer, divid);
                writer.append(appended);
              } catch (IOException e) {
                throw new MustacheException("Failed to write", e, tc);
              }
              // Make a copy of the scopes so we don't change them
              List<Object> scopesCopy = new InternalArrayList<>(scopes);
              deferredCallable.add(
                      new Deferral(divid, getExecutorService().submit(() -> {
                        try {
                          StringWriter sw = new StringWriter();
                          partial.execute(sw, scopesCopy).close();
                          return sw.toString();
                        } catch (IOException e) {
                          throw new MustacheException("Failed to writer", e, tc);
                        }
                      })));
              return writer;
            } else {
              return appendText(partial.execute(writer, scopes));
            }
          }

          private DeferredCallable getDeferred(List<Object> scopes) {
            try {
              if (deferredWrapper == null) {
                deferredWrapper = getObjectHandler().find("deferred", scopes);
              }
              return (DeferredCallable) deferredWrapper.call(scopes);
            } catch (GuardException e) {
              deferredWrapper = null;
              return getDeferred(scopes);
            }
          }
        });
      }
    };
  }

  protected void writeTarget(Writer writer, Long divid) throws IOException {
    writer.append("<div id=\"");
    writer.append(divid.toString());
    writer.append("\"></div>");
  }

  protected static void writeDeferral(StringBuilder sb, Deferral deferral, Object o) {
    sb.append("<script>document.getElementById(\"");
    sb.append(deferral.id);
    sb.append("\").innerHTML=\"");
    sb.append(o.toString().replace("<", "&lt;").replace("\"", "\\\"").replace("\n", "\\n"));
    sb.append("\";</script>");
  }
}
