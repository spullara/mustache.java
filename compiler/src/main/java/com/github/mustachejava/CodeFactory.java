package com.github.mustachejava;

import java.io.Reader;
import java.util.List;

/**
 * Factory for creating codes
 */
public interface CodeFactory {
  // Specified
  Code iterable(String variable, List<Code> codes, String file, int start, String sm, String em);
  Code notIterable(String variable, List<Code> codes, String file, int start, String sm, String em);
  Code name(String variable, List<Code> codes, String file, int start, String sm, String em);
  Code partial(String variable, String file, int line, String sm, String em);
  Code value(String finalName, boolean b, int line, String sm, String em);
  Code write(String text, int line, String sm, String em);

  // Internal
  Code eof(int line);

  // Extension
  Code extend(String variable, List<Code> codes, String file, int start, String sm, String em);
  
  // Get readers
  Reader getReader(String file);
}
