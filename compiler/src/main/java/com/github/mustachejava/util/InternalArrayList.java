package com.github.mustachejava.util;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Used internally for passing around the scopes.
 */
public class InternalArrayList<E> extends ArrayList<E> {
  public InternalArrayList(Collection<? extends E> c) {
    super(c.size());
    addAll(c);
  }

  public InternalArrayList() {
    super();
  }
}
