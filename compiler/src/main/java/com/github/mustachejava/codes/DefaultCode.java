package com.github.mustachejava.codes;

import com.github.mustachejava.*;

import java.io.IOException;
import java.io.Writer;

/**
 * Simplest possible code implementaion with some default shared behavior
 */
public class DefaultCode implements Code, Cloneable {
  // Final once init() is complete
  protected String appended;

  protected Mustache mustache;
  protected final ObjectHandler oh;
  protected final String name;
  protected final TemplateContext tc;
  protected final String type;
  protected final boolean returnThis;
  protected final Binding binding;

  public Object clone() {
    try {
      DefaultCode code = (DefaultCode) super.clone();
      Code[] codes = code.getCodes();
      if (codes != null) {
        codes = codes.clone();
        for (int i = 0; codes != null && i < codes.length; i++) {
          codes[i] = (Code) codes[i].clone();
        }
        code.setCodes(codes);
      }
      if (mustache != null) {
        code.mustache = (Mustache) mustache.clone();
      }
      return code;
    } catch (CloneNotSupportedException e) {
      throw new MustacheException("Clone not supported");
    }
  }

  public DefaultCode() {
    this(null, null, null, null, null);
  }

  public DefaultCode(TemplateContext tc, ObjectHandler oh, Mustache mustache, String name, String type) {
    this.oh = oh;
    this.mustache = mustache;
    this.type = type;
    this.name = name;
    this.tc = tc;
    this.binding = oh == null ? null : oh.createBinding(name, tc, this);
    this.returnThis = ".".equals(name);
  }

  public Code[] getCodes() {
    return mustache == null ? null : mustache.getCodes();
  }

  @Override
  public synchronized void init() {
    Code[] codes = getCodes();
    if (codes != null) {
      for (Code code : codes) {
        code.init();
      }
    }
  }

  public void setCodes(Code[] newcodes) {
    mustache.setCodes(newcodes);
  }

  public Object get(Object[] scopes) {
    if (returnThis) {
      return scopes[scopes.length - 1];
    }
    return binding.get(scopes);
  }

  @Override
  public Writer execute(Writer writer, Object scope) {
    return execute(writer, new Object[]{scope});
  }

  /**
   * The default behavior is to run the codes and append the captured text.
   *
   * @param writer The writer to write the output to
   * @param scopes The scopes to evaluate the embedded names against.
   */
  @Override
  public Writer execute(Writer writer, Object[] scopes) {
    return appendText(runCodes(writer, scopes));
  }

  @Override
  public void identity(Writer writer) {
    try {
      if (name != null) {
        tag(writer, type);
        if (getCodes() != null) {
          runIdentity(writer);
          tag(writer, "/");
        }
      }
      appendText(writer);
    } catch (IOException e) {
      throw new MustacheException(e);
    }
  }

  protected void runIdentity(Writer writer) {
    int length = getCodes().length;
    for (int i = 0; i < length; i++) {
      getCodes()[i].identity(writer);
    }
  }

  protected void tag(Writer writer, String tag) throws IOException {
    writer.write(tc.startChars());
    writer.write(tag);
    writer.write(name);
    writer.write(tc.endChars());
  }

  protected Writer appendText(Writer writer) {
    if (appended != null) {
      try {
        writer.write(appended);
      } catch (IOException e) {
        throw new MustacheException(e);
      }
    }
    return writer;
  }

  protected Writer runCodes(Writer writer, Object[] scopes) {
    Code[] codes = getCodes();
    if (codes != null) {
      for (Code code : codes) {
        writer = code.execute(writer, scopes);
      }
    }
    return writer;
  }

  @Override
  public void append(String text) {
    if (appended == null) {
      appended = text;
    } else {
      appended = appended + text;
    }
  }

  // Expand the current set of scopes
  protected Object[] addScope(Object[] scopes, Object scope) {
    if (scope == null) {
      return scopes;
    } else {
      int length = scopes.length;
      Object[] newScopes = new Object[length + 1];
      System.arraycopy(scopes, 0, newScopes, 0, length);
      newScopes[length] = scope;
      return newScopes;
    }
  }
}
