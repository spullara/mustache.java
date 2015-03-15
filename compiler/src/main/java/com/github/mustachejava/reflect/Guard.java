package com.github.mustachejava.reflect;

import java.util.List;

/**
 * Simple specialization of Predicate
 */
public interface Guard {
  boolean apply(List<Object> input);
}
