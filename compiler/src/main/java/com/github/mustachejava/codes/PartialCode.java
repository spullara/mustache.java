package com.github.mustachejava.codes;

import java.io.Writer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import com.github.mustachejava.Code;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheException;
import com.github.mustachejava.TemplateContext;
import com.github.mustachejava.util.LatchedWriter;
import scala.runtime.StringAdd;

public class PartialCode extends DefaultCode {
  protected Mustache partial;
  protected final String variable;
  protected final String extension;
  private DefaultMustacheFactory cf;
  private final ExecutorService les;

  protected PartialCode(TemplateContext tc, DefaultMustacheFactory cf, Mustache mustache, String type, String variable) {
    super(tc, cf.getObjectHandler(), mustache, variable, type);
    this.cf = cf;
    this.variable = variable;
    // Use the  name of the parent to get the name of the partial
    int index = tc.file().lastIndexOf(".");
    extension = index == -1 ? "" : tc.file().substring(index);
    les = cf.getExecutorService();
  }

  public PartialCode(TemplateContext tc, DefaultMustacheFactory cf, String variable) {
    this(tc, cf, null, ">", variable);
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
    partial = cf.compile(buildFileName());
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

  /**
   * Builds the file name to be included by this partial tag. Default implementation ppends the tag contents with
   * the current file's extension.
   * @return The filename to be included by this partial tag
   */
  protected String buildFileName() {
      return variable + extension;
  }
}
