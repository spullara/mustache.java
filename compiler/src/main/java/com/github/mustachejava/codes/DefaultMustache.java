package com.github.mustachejava.codes;

import com.github.mustachejava.Code;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.TemplateContext;
import com.github.mustachejava.util.InternalArrayList;
import com.github.mustachejava.util.Node;

import java.io.Writer;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Default Mustache
 */
public class DefaultMustache extends DefaultCode implements Mustache {
  private Code[] codes;
  private boolean inited = false;

  public DefaultMustache(TemplateContext tc, DefaultMustacheFactory df, Code[] codes, String name) {
    super(tc, df, null, name, null);
    setCodes(codes);
  }

  @Override
  public Code[] getCodes() {
    return codes;
  }

  public Writer run(Writer writer, List<Object> scopes) {
    if (codes != null) {
      for (Code code : codes) {
        writer = code.execute(writer, scopes);
      }
    }
    return writer;
  }

  @Override
  public Node invert(String text) {
    return invert(new Node(), text, new AtomicInteger(0));
  }

  @Override
  public void setCodes(Code[] newcodes) {
    codes = newcodes;
  }

  @Override
  public Writer execute(Writer writer, List<Object> scopes) {
    if (!(scopes instanceof InternalArrayList)) {
      // This is the only place where we explicitly allocate post initialization for a compiled mustache
      // in order to track the scopes as you descend the template. It ends up being ~200 bytes.
      scopes = new InternalArrayList<>(scopes);
    }
    return super.execute(writer, scopes);
  }

  @Override
  public void identity(Writer writer) {
    // No self output at the top level
    runIdentity(writer);
  }

  @Override
  public synchronized void init() {
    if (!inited) {
      inited = true;
      super.init();
    }
  }


  // Used for an optimization in PartialCode
  private boolean isRecursive = false;

  public boolean isRecursive() {
    return isRecursive;
  }

  public void setRecursive() {
    isRecursive = true;
  }
}
