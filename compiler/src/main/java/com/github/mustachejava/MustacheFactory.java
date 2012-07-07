package com.github.mustachejava;

import java.io.Reader;
import java.io.Writer;

/**
 * Factory for creating codes
 */
public interface MustacheFactory {
  /**
   * Creates the visitor for compilation.
   *
   * @return visitor
   */
  MustacheVisitor createMustacheVisitor();

  /**
   * Given a resource name, construct a reader.
   *
   * @param resourceName used to find the resource
   * @return a reader
   */
  Reader getReader(String resourceName);

  /**
   * This defines how "encoded" values are encoded. It defaults to
   * something appropriate for HTML output.
   *
   * @param value  the unencoded value
   * @param writer where to encode the value
   */
  void encode(String value, Writer writer);

  /**
   * The object handler knows how to transform names into fields and methods.
   *
   * @return the handler
   */
  ObjectHandler getObjectHandler();

  /**
   * Create a mustache given a resource name.
   *
   * @param name the name of the resource
   * @return the compiled mustache
   */
  Mustache compile(String name);

  /**
   * Create a mustache given a reader and a name.
   *
   * @param reader the reader
   * @param name   the name of the resource
   * @return the compiled mustache
   */
  Mustache compile(Reader reader, String name);

  /**
   * Converts your arbitrary name to another name.
   *
   * @param from the tag to replace
   * @return the new tag
   */
  String translate(String from);
}
