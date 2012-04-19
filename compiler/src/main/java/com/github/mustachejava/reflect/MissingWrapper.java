package com.github.mustachejava.reflect;

import java.util.List;

import com.google.common.base.Predicate;

/**
 * Used to mark a wrapper this is only guarding a complete miss.
 */
public class MissingWrapper extends GuardedWrapper {
  public MissingWrapper(Predicate[] guard) {
    super(guard);
  }
}
