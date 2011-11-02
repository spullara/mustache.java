package com.sampullara.mustache;

import com.sampullara.util.FutureWriter;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Codes for mustache.
 * <p/>
 * User: sam
 * Date: 5/15/11
 * Time: 1:26 PM
 */
public interface Code {
  /**
   * Execute the code and write to the future writer using scope.
   *
   * @param fw
   * @param scope
   * @throws MustacheException
   */
  void execute(FutureWriter fw, Scope scope) throws MustacheException;

  /**
   * The first line for the code element.
   *
   * @return
   */
  int getLine();

  /**
   * Attempts to parse at the current position. Either returns the current Scope object
   * containing the unexecuted data or null to indicate it was not a match. We avoid
   * using a Reader for simplicity over efficiency since this isn't a high
   * performance API but typically for development or test time.
   *
   * @param current scope that we are currently using
   * @param text full text to parse
   * @param position where are we in the text for this unexecute, updated post execution
   * @param next subsequent code so we can probe for the end of this code
   * @return the current scope object if successful, otherwise null
   * @throws MustacheException if the text does not match the template
   */
  Scope unexecute(Scope current, String text, AtomicInteger position, Code[] next) throws MustacheException;

  /**
   * Return the template that created this code.
   *
   * @param fw
   * @throws MustacheException
   */
  void identity(FutureWriter fw) throws MustacheException;
}

