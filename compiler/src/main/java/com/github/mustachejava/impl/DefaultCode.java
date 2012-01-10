package com.github.mustachejava.impl;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import com.github.mustachejava.Code;
import com.github.mustachejava.MustacheException;
import com.github.mustachejava.ObjectHandler;

/**
 * Simplest possible code implementaion with some default shared behavior
 */
public class DefaultCode implements Code {
  private StringBuilder sb = new StringBuilder();
  protected String appended;

  protected final ObjectHandler oh;
  protected final Code[] codes;
  protected final String name;
  protected final String type;
  protected final String sm;
  protected final String em;

  // Callsite caching
  protected MethodWrapper methodWrapper;

  // TODO: Recursion protection. Need better guard logic. But still fast.
  protected int numScopes = -1;

  public DefaultCode() {
    this(null, null, null, null, null, null);
  }

  public DefaultCode(ObjectHandler oh, Code[] codes, String name, String type, String sm, String em) {
    this.oh = oh;
    this.codes = codes;
    this.type = type;
    this.name = name;
    this.sm = sm;
    this.em = em;
  }

  public Object resolve(String name, Object... scopes) {
    if (numScopes == -1) numScopes = scopes.length;
    if (name.equals(".")) {
      return scopes[scopes.length - 1];
    }
    if (scopes.length != numScopes || methodWrapper == null) {
      methodWrapper = oh.find(name, scopes);
    }
    try {
      return methodWrapper == null ? null : methodWrapper.call(scopes);
    } catch (MethodGuardException e) {
      methodWrapper = null;
      return resolve(name, scopes);
    }
  }

  /**
   * Default append behavior
   * @param writer
   * @param scopes
   */
  @Override
  public void execute(Writer writer, Object... scopes) {
    runCodes(writer, scopes);
    appendText(writer);
  }

  @Override
  public void identity(Writer writer) {
    try {
      if (name != null) {
        tag(writer, type);
        if (codes != null) {
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
    int length = codes.length;
    for (int i = 0; i < length; i++) {
      codes[i].identity(writer);
    }
  }

  private void tag(Writer writer, String tag) throws IOException {
    writer.write(sm);
    writer.write(tag);
    writer.write(name);
    writer.write(em);
  }

  protected void appendText(Writer writer) {
    if (appended != null) {
      try {
        writer.write(appended);
      } catch (IOException e) {
        throw new MustacheException(e);
      }
    }
  }

  protected void runCodes(Writer writer, Object... scopes) {
    if (codes != null) {
      int length = codes.length;
      for (int i = 0; i < length; i++) {
        codes[i].execute(writer, scopes);
      }
    }
  }

  @Override
  public void append(String text) {
    sb.append(text);
    appended = sb.toString();
  }
}
