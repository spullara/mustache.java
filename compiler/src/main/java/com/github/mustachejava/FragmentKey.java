package com.github.mustachejava;

/**
 * Used for indexing runtime compiled template text from lambdas.
 */
public class FragmentKey {
  public final TemplateContext tc;
  public final String templateText;

  public FragmentKey(TemplateContext tc, String templateText) {
    this.tc = tc;
    this.templateText = templateText;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    FragmentKey that = (FragmentKey) o;
    return tc.equals(that.tc) && templateText.equals(that.templateText);
  }

  @Override
  public int hashCode() {
    int result = tc.hashCode();
    result = 31 * result + templateText.hashCode();
    return result;
  }
}
