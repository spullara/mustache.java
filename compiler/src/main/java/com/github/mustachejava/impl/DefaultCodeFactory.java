package com.github.mustachejava.impl;

import java.util.List;

import com.github.mustachejava.Code;
import com.github.mustachejava.CodeFactory;
import com.github.mustachejava.Mustache;

/**
 * Simplest possible code factory
 */
public class DefaultCodeFactory implements CodeFactory {
  @Override
  public Code iterable(Mustache m, String variable, List<Code> codes, String file, int start) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public Code notIterable(Mustache m, String variable, List<Code> codes, String file, int start) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public Code name(Mustache m, String variable, List<Code> codes, String file, int start) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public Code partial(Mustache m, String variable, String file, int line) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public Code value(Mustache m, String finalName, boolean b, int line) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public Code write(String text, int line) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public Code eof(int line) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public Code extend(Mustache m, String variable, List<Code> codes, String file, int start) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }
}
