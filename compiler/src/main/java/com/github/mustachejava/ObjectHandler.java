package com.github.mustachejava;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * TODO: Edit this
 * <p/>
 * User: sam
 * Date: 7/24/11
 * Time: 2:59 PM
 */
public interface ObjectHandler {
  public static ArrayList EMPTY = new ArrayList(0);
  public static Object NULL = new Object() {
    @Override
    public String toString() {
      return "";
    }
  };

  Object handleObject(Object scope, String name);
  Iterator iterate(Object object);
}
