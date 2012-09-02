package com.github.mustachejava.reflect;

/**
 * Used to mark a wrapper this is only guarding a complete miss.
 */
public class MissingWrapper extends GuardedWrapper {
  public MissingWrapper(Guard[] guard) {
    super(guard);
  }
}
