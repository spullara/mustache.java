package com.github.mustachejava;

import com.github.mustachejava.util.InternalArrayList;
import com.github.mustachejava.util.Node;

import java.io.Writer;
import java.util.List;

import static com.github.mustachejava.ObjectHandler.makeList;
import static java.util.Collections.addAll;

/**
 * The interface to Mustache objects
 */
public interface Mustache extends Code {
  /**
   * Append text to the mustache output.
   * 
   * @param text the text to append
   */
  void append(String text);

  /**
   * Deep clone of the mustache object.
   *
   * @return the clone
   */
  Object clone();

  /**
   * Execute the mustache object with a given writer and a single scope context.
   *
   * @param writer write the output of the executed template here
   * @param scope the root object to use
   * @return the new writer
   */
  default Writer execute(Writer writer, Object scope) {
    return execute(writer, makeList(scope));
  }

  // Support the previous behavior for users
  default Writer execute(Writer writer, Object[] scopes) {
    List<Object> newscopes = new InternalArrayList<>();
    addAll(newscopes, scopes);
    return execute(writer, newscopes);
  }

  /**
   * Execute the mustache with a given writer and an array of scope objects. The
   * scopes are searched right-to-left for references.
   *
   * @param writer write the output of the executed template here
   * @param scopes an ordered list of scopes for variable resolution
   * @return the new writer
   */
  Writer execute(Writer writer, List<Object> scopes);

  /**
   * Get the underlying code objects.
   *
   * @return the array of child codes
   */
  Code[] getCodes();

  /**
   * Execute the mustache to output itself.
   *
   * @param writer write the output of the executed template here
   */
  void identity(Writer writer);

  /**
   * Initialize the mustache before executing. This is must be called at least once
   * and is normally called already by the time you have a mustache instance.
   */
  void init();

  /**
   * Change the underlying codes of the mustache implementation.
   *  
   * @param codes set the children of this code 
   */
  void setCodes(Code[] codes);

  /**
   * Only executes the codes. Does not append the text.
   *
   * @param writer write the output of the executed template here
   * @param scopes the array of scopes to execute
   * @return the replacement writer
   */
  Writer run(Writer writer, List<Object> scopes);

  /**
   * Invert this mustache given output text.
   *
   * @param text the text to parse
   * @return a tree of nodes representing the variables that when passed as a scope would reproduce the text
   */
  Node invert(String text);

}