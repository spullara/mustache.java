package com.github.mustachejava;

import com.github.mustachejava.util.Node;

import java.io.Writer;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Code objects that are executed in order to evaluate the template
 */
public interface Code {
  Writer execute(Writer writer, List<Object> scopes);

  void identity(Writer writer);

  void append(String text);

  Code[] getCodes();

  void setCodes(Code[] codes);

  void init();

  @SuppressWarnings("CloneDoesntDeclareCloneNotSupportedException")
  Object clone();

  Object clone(Set<Code> seen);

  String getName();

  Node invert(Node node, String text, AtomicInteger position);
}
