package com.github.mustachejava;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

/**
* Created by IntelliJ IDEA.
* User: spullara
* Date: 1/17/12
* Time: 8:19 PM
* To change this template use File | Settings | File Templates.
*/
public class ParallelComplexObject {
  String header = "Colors";
  Callable<List<Color>> item = () -> Arrays.asList(
          new Color("red", true, "#Red"),
          new Color("green", false, "#Green"),
          new Color("blue", false, "#Blue"));

  boolean list() {
    return true;
  }

  boolean empty() {
    return false;
  }

  private static class Color {
    boolean link() {
      return !current;
    }

    Color(String name, boolean current, String url) {
      this.name = name;
      this.current = current;
      this.url = url;
    }

    String name;
    boolean current;
    String url;
  }
}
