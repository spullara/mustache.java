package com.github.mustachejava;

import java.io.Reader;
import java.util.List;

/**
 * Factory for creating codes
 */
public interface CodeFactory {
  // Specified
  Code iterable(String variable, List<Code> codes, String file, int start);
  Code notIterable(String variable, List<Code> codes, String file, int start);
  Code name(String variable, List<Code> codes, String file, int start);
  Code partial(String variable, String file, int line);
  Code value(String finalName, boolean b, int line);
  Code write(String text, int line);

  // Internal
  Code eof(int line);

  // Extension
  Code extend(String variable, List<Code> codes, String file, int start);
  
  // Get readers
  Reader getReader(String file);
}
