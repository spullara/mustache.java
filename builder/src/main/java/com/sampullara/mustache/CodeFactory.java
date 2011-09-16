package com.sampullara.mustache;

import java.util.List;

/**
* We need to make the code factory replaceable
*/
public interface CodeFactory {
  Code iterable(Mustache m, String variable, List<Code> codes, String file, int line);
  Code function(Mustache m, String variable, List<Code> codes, String file, int line);
  Code ifIterable(Mustache m, String variable, List<Code> codes, String file, int line);
  Code notIterable(Mustache m, String variable, List<Code> codes, String file, int line);
  Code partial(Mustache m, String variable, String file, int line);
  Code value(Mustache m, String name, boolean encode, int line);
  Code write(String s, int line);
  Code eof(int line);
}
