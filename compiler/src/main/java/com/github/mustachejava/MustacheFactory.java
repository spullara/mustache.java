package com.github.mustachejava;

import java.io.Reader;
import java.util.List;

/**
 * Factory for creating codes
 */
public interface MustacheFactory {
  // Specified
  Code iterable(String variable, List<Code> codes, String file, int start, String sm, String em);
  Code notIterable(String variable, List<Code> codes, String file, int start, String sm, String em);
  Code name(String variable, List<Code> codes, String file, int start, String sm, String em);
  Code partial(String variable, String file, int line, String sm, String em);
  Code value(String finalName, boolean b, int line, String sm, String em);
  Code write(String text, int line, String sm, String em);

  // Internal
  Code eof(int line);

  // TODO: Extension
  Code extend(String variable, List<Code> codes, String file, int start, String sm, String em);
  
  // Get readers
  Reader getReader(String file);

  // Override this in a super class if you don't want encoding or would like
  // to change the way encoding works. Also, if you use unexecute, make sure
  // also do the inverse in decode.
  String encode(String value);

  // Find objects
  ObjectHandler getObjectHandler();

  // Create mustaches
  Mustache compile(String name);
  Mustache compile(Reader reader, String name, String sm, String em);

  // Translate one command to another for extensions
  String translate(String from);
}
