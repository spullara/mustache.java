package com.github.mustachejavabenchmarks;

import com.fasterxml.jackson.core.JsonGenerator;
import com.github.mustachejava.util.CapturingMustacheVisitor;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

/**
 * Flawed implementation -- doesn't handle the case when you do a negative compare and then
 * iterate. Works fine if you iterate first.
 */
public class JsonCapturer implements CapturingMustacheVisitor.Captured {
  private Stack<Set<String>> seenit;
  private int ignore;
  private final JsonGenerator jg;

  public JsonCapturer(JsonGenerator jg) {
    this.jg = jg;
    seenit = new Stack<>();
    ignore = 0;
    seenit.add(new HashSet<>());
  }

  @Override
  public void value(String name, String value) {
    if (ignore > 0) return;
    try {
      if (seenit.peek().add(name)) {
        jg.writeStringField(name, value);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void arrayStart(String name) {
    if (ignore > 0) return;
    try {
      if (seenit.peek().add(name)) {
        jg.writeArrayFieldStart(name);
      } else {
        ignore++;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void arrayEnd() {
    if (ignore > 0) {
      ignore--;
      return;
    }
    try {
      jg.writeEndArray();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void objectStart() {
    if (ignore > 0) return;
    try {
      jg.writeStartObject();
      seenit.push(new HashSet<>());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void objectEnd() {
    try {
      jg.writeEndObject();
      seenit.pop();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
