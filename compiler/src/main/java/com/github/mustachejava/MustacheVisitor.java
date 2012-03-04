package com.github.mustachejava;

import java.util.List;

/**
 * Callbacks from the parser as a mustache template is parsed.
 */
public interface MustacheVisitor {
  // Mustache
  Mustache mustache(String file, String sm, String em);

  // Specified
  void iterable(String variable, Mustache mustache, String file, int start, String sm, String em);
  void notIterable(String variable, Mustache mustache, String file, int start, String sm, String em);
  void partial(String variable, String file, int line, String sm, String em);
  void value(String finalName, boolean b, int line, String sm, String em);
  void write(String text, int line, String sm, String em);

  // Internal
  void eof(int line);

  // Extension
  void extend(String variable, Mustache mustache, String file, int start, String sm, String em);
  void name(String variable, Mustache mustache, String file, int start, String sm, String em);
}
