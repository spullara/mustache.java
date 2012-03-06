package com.github.mustachejava.codes;

import java.io.Writer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import com.github.mustachejava.Code;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheException;
import com.github.mustachejava.util.LatchedWriter;

public class PartialCode extends DefaultCode {
  protected Mustache partial;
  private final String variable;
  protected final String extension;
  private DefaultMustacheFactory cf;
  private final ExecutorService les;

  protected PartialCode(DefaultMustacheFactory cf, Mustache mustache, String type, String variable, String file, String sm, String em) {
    super(cf.getObjectHandler(), mustache, variable, type, sm, em);
    this.cf = cf;
    this.variable = variable;
    // Use the  name of the parent to get the name of the partial
    int index = file.lastIndexOf(".");
    extension = index == -1 ? "" : file.substring(index);
    les = cf.getExecutorService();
  }

  public PartialCode(DefaultMustacheFactory cf, String variable, String file, String sm, String em) {
    this(cf, null, ">", variable, file, sm, em);
  }

  @Override
  public Code[] getCodes() {
    return partial.getCodes();
  }

  @Override
  public void setCodes(Code[] newcodes) {
    partial.setCodes(newcodes);
  }

  @Override
  public Writer execute(Writer writer, final Object[] scopes) {
    return partialExecute(writer, scopes);
  }

  @Override
  public synchronized void init() {
    partial = cf.compile(variable + extension);
    if (partial == null) {
      throw new MustacheException("Failed to compile partial: " + variable);
    }
  }

  protected Writer partialExecute(Writer writer, final Object[] scopes) {
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

  protected Writer execute(Writer writer, Object scope, Object[] scopes) {
    Object[] newscopes = addScope(scope, scopes);
    return appendText(partial.execute(writer, newscopes));
  }
}
