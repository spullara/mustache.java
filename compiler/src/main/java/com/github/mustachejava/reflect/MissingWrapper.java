package com.github.mustachejava.reflect;

import static java.util.Arrays.asList;

/**
 * Used to mark a wrapper this is only guarding a complete miss.
 */
public class MissingWrapper extends GuardedWrapper {
  private final String name;

  public MissingWrapper(String name, Guard[] guards) {
    super(guards);
    this.name = name;
  }

  public String toString() {
    return "[Missing: " + name + " Guards: " + asList(guards) + "]";
  }
}
