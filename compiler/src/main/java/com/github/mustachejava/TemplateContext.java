package com.github.mustachejava;

public class TemplateContext {
  private final String sm;
  private final String em;
  private final String file;
  private final int line;

  public TemplateContext(String sm, String em, String file, int line) {
    this.sm = sm;
    this.em = em;
    this.file = file;
    this.line = line;
  }

  public String startChars() {
    return sm;
  }

  public String endChars() {
    return em;
  }

  public String file() {
    return file;
  }

  public int line() {
    return line;
  }
}
