package com.github.mustachejava;

import java.util.ArrayList;
import java.util.Iterator;

import com.github.mustachejava.util.MethodWrapper;

/**
 * TODO: Edit this
 * <p/>
 * User: sam
 * Date: 7/24/11
 * Time: 2:59 PM
 */
public interface ObjectHandler {
  // Find methods to call
  MethodWrapper find(String name, Object... scopes);

  // Coerce results to Java native iterables, functions, callables
  Object coerce(Object object);
}
