package com.github.mustachejava;

import java.io.Reader;
import java.io.Writer;
import java.util.List;

/**
 * Factory for creating codes
 */
public interface MustacheFactory {
  // Create a new visitor
  MustacheVisitor createMustacheVisitor();

  // Get readers
  Reader getReader(String file);

  // This defines how "encoded" values are encoded. It defaults to
  // something appropriate for HTML output.
  void encode(String value, Writer writer);

  // Find objects
  ObjectHandler getObjectHandler();

  // Create mustaches
  Mustache compile(String name);
  Mustache compile(Reader reader, String name, String sm, String em);

  // Translate one command to another for extensions
  String translate(String from);
}
