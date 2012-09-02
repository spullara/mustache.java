package com.github.mustachejava.reflect;

/**
 * Simple specialization of Predicate
 */
public interface Guard {
  boolean apply(Object[] input);
}
