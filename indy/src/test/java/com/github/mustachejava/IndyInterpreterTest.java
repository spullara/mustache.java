package com.github.mustachejava;

import com.github.mustachejava.indy.IndyObjectHandler;

public class IndyInterpreterTest extends InterpreterTest {
  @Override
  protected DefaultMustacheFactory createMustacheFactory() {
    DefaultMustacheFactory mustacheFactory = super.createMustacheFactory();
    mustacheFactory.setObjectHandler(new IndyObjectHandler());
    return mustacheFactory;
  }
}
