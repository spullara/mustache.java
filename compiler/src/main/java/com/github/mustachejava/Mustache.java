package com.github.mustachejava;

import java.io.Writer;

/**
 * The interface to Mustache objects
 */
public interface Mustache extends Code {
  /**
   * Append text to the mustache output.
   * @param text
   */
  void append(String text);

  /**
   * Deep clone of the mustache object.
   * @return
   */
  Object clone();

  /**
   * Execute the mustache object with a given writer and a single scope context.
   * @param writer
   * @param scope
   * @return
   */
  Writer execute(Writer writer, Object scope);

  /**
   * Execute the mustache with a given writer and an array of scope objects. The
   * scopes are searched right-to-left for references.
   * @param writer
   * @param scopes
   * @return
   */
  Writer execute(Writer writer, Object[] scopes);

  /**
   * Get the underlying code objects.
   * @return
   */
  Code[] getCodes();

  /**
   * Execute the mustache to output itself.
   * @param writer
   */
  void identity(Writer writer);

  /**
   * Initialize the mustache before executing. This is must be called at least once
   * and is normally called already by the time you have a mustache instance.
   */
  void init();

  /**
   * Change the underlying codes of the mustache implementation.
   * @param codes
   */
  void setCodes(Code[] codes);
}
