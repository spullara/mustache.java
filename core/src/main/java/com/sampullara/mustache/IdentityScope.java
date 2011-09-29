package com.sampullara.mustache;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * Used to reproduce markup from code.
 */
public class IdentityScope extends Scope {
  public final static IdentityScope one = new IdentityScope();

  private IdentityScope() {}

  @Override
  public Object get(Object o) {
    return one;
  }
}
