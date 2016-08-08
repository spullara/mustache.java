package com.github.mustachejava.codes;

import com.github.mustachejava.Binding;
import com.github.mustachejava.Code;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheException;
import com.github.mustachejava.ObjectHandler;
import com.github.mustachejava.TemplateContext;
import com.github.mustachejava.util.Node;

import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

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
  protected final DefaultMustacheFactory df;

  @SuppressWarnings({"CloneDoesntCallSuperClone", "CloneDoesntDeclareCloneNotSupportedException"})
  public Object clone() {
    Set<Code> seen = new HashSet<>();
    seen.add(this);
    return clone(seen);
  }

  public Object clone(Set<Code> seen) {
    try {
      DefaultCode code = (DefaultCode) super.clone();
      Code[] codes = code.getCodes();
      if (codes != null) {
        // Create a new set of codes
        codes = codes.clone();
        for (int i = 0; i < codes.length; i++) {
          // If the code hasn't been seen before
          // use this one, else clone it.
          if (!seen.add(codes[i])) {
            codes[i] = (Code) codes[i].clone(seen);
            seen.remove(codes[i]);
          }
        }
        code.setCodes(codes);
      }
      if (mustache != null) {
        if (!seen.add(mustache)) {
          code.mustache = (Mustache) mustache.clone(seen);
          seen.remove(mustache);
        }
      }
      return code;
    } catch (CloneNotSupportedException e) {
      throw new MustacheException("Clone not supported", e, tc);
    }
  }

  public DefaultCode() {
    this(null, null, null, null, null);
  }

  public DefaultCode(TemplateContext tc, DefaultMustacheFactory df, Mustache mustache, String name, String type) {
    this.df = df;
    this.oh = df == null ? null : df.getObjectHandler();
    this.mustache = mustache;
    this.type = type;
    this.name = name;
    this.tc = tc;
    this.binding = oh == null ? null : oh.createBinding(name, tc, this);
    this.returnThis = ".".equals(name);
  }

  @Override
  public Node invert(Node node, String text, AtomicInteger position) {
    int start = position.get();
    Code[] codes = getCodes();
    if (codes != null) {
      for (Code code : codes) {
        Node invert = code.invert(node, text, position);
        if (invert == null) {
          position.set(start);
          return null;
        }
      }
    }
    return matchAppended(node, text, position, start);
  }

  protected Node matchAppended(Node node, String text, AtomicInteger position, int start) {
    if (appended == null) {
      return node;
    } else if (text.substring(position.get()).startsWith(appended)) {
      position.addAndGet(appended.length());
      return node;
    } else {
      position.set(start);
      return null;
    }
  }

  public Code[] getCodes() {
    return mustache == null ? null : mustache.getCodes();
  }

  @Override
  public synchronized void init() {
    filterText();
    Code[] codes = getCodes();
    if (codes != null) {
      for (Code code : codes) {
        code.init();
      }
    }
  }

  protected void filterText() {
    if (df != null && appended != null) {
      appended = df.filterText(appended, tc.startOfLine());
    }
  }

  public void setCodes(Code[] newcodes) {
    mustache.setCodes(newcodes);
  }

  public Object get(List<Object> scopes) {
    if (returnThis) {
      int length = scopes == null ? 0 : scopes.size();
      return length == 0 ? null : scopes.get(length - 1);
    }
    try {
      return binding.get(scopes);
    } catch (MustacheException e) {
      e.setContext(tc);
      throw e;
    } catch (Throwable e) {
      throw new MustacheException(e.getMessage(), e, tc);
    }
  }

  /**
   * The default behavior is to run the codes and append the captured text.
   *
   * @param writer The writer to write the output to
   * @param scopes The scopes to evaluate the embedded names against.
   */
  @Override
  public Writer execute(Writer writer, List<Object> scopes) {
    return appendText(run(writer, scopes));
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
      throw new MustacheException("Failed to write", e, tc);
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

  private char[] appendedChars;
  
  protected Writer appendText(Writer writer) {
    if (appended != null) {
      try {
        // Avoid allocations at runtime
        if (appendedChars == null) {
          appendedChars = appended.toCharArray();
        }
        writer.write(appendedChars);
      } catch (IOException e) {
        throw new MustacheException("Failed to write", e, tc);
      }
    }
    return writer;
  }

  protected Writer run(Writer writer, List<Object> scopes) {
    return mustache == null ? writer : mustache.run(writer, scopes);
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
  protected boolean addScope(List<Object> scopes, Object scope) {
    if (scope != null) {
      scopes.add(scope);
      return true;
    }
    return false;
  }

  @Override
  public String getName() {
    return name;
  }

}
