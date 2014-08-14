package com.github.mustachejava;

import com.github.mustachejava.util.Node;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Code objects that are executed in order to evaluate the template
 */
public interface Code {
  Writer execute(Writer writer, Object scope);

  Writer execute(Writer writer, Object[] scopes);

  void identity(Writer writer);

  void append(String text);

  Code[] getCodes();

  void setCodes(Code[] codes);

  void init();

  Object clone();

  Object clone(Set<Code> seen);

  String getName();

  /**
   * If it returns a node, that means that it successfully parsed it
   * and advanced the reader.
   *
   * @param reader
   * @return
   */
  Node invert(Node node, String text, AtomicInteger position);
}
