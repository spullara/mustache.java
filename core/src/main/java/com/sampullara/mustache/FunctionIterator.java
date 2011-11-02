package com.sampullara.mustache;

import com.google.common.base.Function;

import com.sampullara.util.TemplateFunction;

/**
 * Used to reproduce markup from code.
 */
public abstract class FunctionIterator implements Iterable<Scope> {
  private boolean isTemplateFunction;

  public FunctionIterator(Function function) {
    isTemplateFunction = function instanceof TemplateFunction;
  }

  public boolean isTemplateFunction() {
    return isTemplateFunction;
  }
}
