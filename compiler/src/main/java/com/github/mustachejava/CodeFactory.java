package com.github.mustachejava;

import java.util.List;

/**
 * Factory for creating codes
 */
public interface CodeFactory {
  // Specified
  Code iterable(Mustache m, String variable, List<Code> codes, String file, int start);
  Code notIterable(Mustache m, String variable, List<Code> codes, String file, int start);
  Code name(Mustache m, String variable, List<Code> codes, String file, int start);
  Code partial(Mustache m, String variable, String file, int line);
  Code value(Mustache m, String finalName, boolean b, int line);
  Code write(String text, int line);

  // Internal
  Code eof(int line);

  // Extension
  Code extend(Mustache m, String variable, List<Code> codes, String file, int start);
}
