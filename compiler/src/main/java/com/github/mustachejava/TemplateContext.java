package com.github.mustachejava;

public final class TemplateContext {
  private final String sm;
  private final String em;
  private final String file;
  private final int line;
  private final boolean startOfLine;

  public TemplateContext(String sm, String em, String file, int line, boolean startOfLine) {
    this.sm = sm;
    this.em = em;
    this.file = file;
    this.line = line;
    this.startOfLine = startOfLine;
  }

  public boolean startOfLine() {
    return startOfLine;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TemplateContext that = (TemplateContext) o;
    return line == that.line && 
            !(em != null ? !em.equals(that.em) : that.em != null) && 
            !(file != null ? !file.equals(that.file) : that.file != null) && 
            !(sm != null ? !sm.equals(that.sm) : that.sm != null);
  }

  @Override
  public int hashCode() {
    int result = sm != null ? sm.hashCode() : 0;
    result = 31 * result + (em != null ? em.hashCode() : 0);
    result = 31 * result + (file != null ? file.hashCode() : 0);
    result = 31 * result + line;
    return result;
  }

  public String toString() {
    return "[" + file + ":" + line + "]";
  }
}
