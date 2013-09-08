package com.github.mustachejava.codes;

import com.github.mustachejava.Code;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.Node;
import com.github.mustachejava.TemplateContext;

import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Default Mustache
 */
public class DefaultMustache extends DefaultCode implements Mustache {
  private Code[] codes;
  private boolean inited = false;

  @Override
  public Node invert(Node node, String text, AtomicInteger position) throws IOException {
    int start = position.get();
    for (Code code : codes) {
      Node invert = code.invert(node, text, position);
      if (invert == null) {
        position.set(start);
        return null;
      }
    }
    return node;
  }

  public DefaultMustache(TemplateContext tc, DefaultMustacheFactory df, Code[] codes, String name) {
    super(tc, df, null, name, null);
    setCodes(codes);
  }

  @Override
  public Code[] getCodes() {
    return codes;
  }

  public Writer run(Writer writer, Object[] scopes) {
    if (codes != null) {
      for (Code code : codes) {
        writer = code.execute(writer, scopes);
      }
    }
    return writer;
  }

  @Override
  public void setCodes(Code[] newcodes) {
    codes = newcodes;
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
}
