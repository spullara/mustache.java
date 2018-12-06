package com.github.mustachejava.util;

import java.util.List;
import java.util.Objects;

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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    NodeValue nodeValue = (NodeValue) o;
    return Objects.equals(list, nodeValue.list) &&
            Objects.equals(value, nodeValue.value);
  }

  @Override
  public int hashCode() {
    int result = list != null ? list.hashCode() : 0;
    result = 31 * result + (value != null ? value.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return isList ? list.toString() : value;
  }
}
