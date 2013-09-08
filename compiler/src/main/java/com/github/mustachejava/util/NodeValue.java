package com.github.mustachejava.util;

import java.util.List;

public class NodeValue {
  private final List<Node> list;
  private final String value;
  private final boolean isList;

  private NodeValue(List<Node> list, String value) {
    this.list = list;
    isList = list != null;
    this.value = value;
  }

  public static NodeValue list(List<Node> list) {
    return new NodeValue(list, null);
  }

  public static NodeValue value(String value) {
    return new NodeValue(null, value);
  }

  public boolean isList() {
    return isList;
  }

  public List<Node> list() {
    return list;
  }

  public String value() {
    return value;
  }
}
