package com.sampullara.mustache.code;

import com.sampullara.mustache.Code;
import com.sampullara.mustache.CodeFactory;
import com.sampullara.mustache.Mustache;
import com.sampullara.mustache.MustacheException;
import com.sampullara.mustache.Scope;

import java.util.List;

/**
 * The builder code factory is designed to work well with the default Mustache implementation.
 * In many cases where you significantly change the behavior of the default Mustache class you
 * may want to have your own CodeFactory that works with it.
 */
public class BuilderCodeFactory implements CodeFactory {

  @Override
  public Code iterable(Mustache m, String variable, List<Code> codes, String file, int line) {
    return new IterableCode(m, variable, codes, file, line);
  }

  @Override
  public Code function(Mustache m, String variable, List<Code> codes, String file, int line) {
    return new FunctionCode(m, variable, codes, file, line);
  }

  @Override
  public Code ifIterable(Mustache m, String variable, List<Code> codes, String file, int line) {
    return new IfIterableCode(m, variable, codes, file, line);
  }

  @Override
  public Code notIterable(Mustache m, String variable, List<Code> codes, String file, int line) {
    return new InvertedIterableCode(m, variable, codes, file, line);
  }

  @Override
  public Code partial(Mustache m, String variable, String file, int line) throws MustacheException {
    return new PartialCode(m, variable, file, line);
  }

  @Override
  public Code value(Mustache m, String name, boolean encode, int line) {
    return new WriteValueCode(m, name, encode, line);
  }

  @Override
  public Code write(String s, int line) {
    return new DefaultWriteCode(s, line);
  }

  @Override
  public Code eof(int line) {
    return new EOFCode(line);
  }

  @Override
  public Code extend(Mustache m, String variable, List<Code> codes, String file, int i) throws MustacheException {
    return new ExtendCode(m, variable, codes, file, i);
  }

  @Override
  public Code name(Mustache m, String variable, List<Code> codes, String file, int i) {
    return new ExtendNameCode(m, variable, codes, file, i);
  }

  public static void put(Scope result, String name, Object value) {
    String[] splits = name.split("[.]");
    Scope depth = result;
    for (int i = 0; i < splits.length; i++) {
      if (i < splits.length - 1) {
        Scope tmp = (Scope) result.get(splits[i]);
        if (tmp == null) {
          tmp = new Scope();
        }
        depth.put(splits[i], tmp);
        depth = tmp;
      } else {
        depth.put(splits[i], value);
      }
    }
  }

}
