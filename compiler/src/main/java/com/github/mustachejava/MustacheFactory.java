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

  // Override this in a super class if you don't want encoding or would like
  // to change the way encoding works. Also, if you use unexecute, make sure
  // also do the inverse in decode.
  void encode(String value, Writer writer);

  // Find objects
  ObjectHandler getObjectHandler();

  // Create mustaches
  Mustache compile(String name);
  Mustache compile(Reader reader, String name, String sm, String em);

  // Translate one command to another for extensions
  String translate(String from);
}
