package com.github.mustachejava;

import com.github.mustachejava.codegen.CodegenMustacheFactory;
import com.github.mustachejava.indy.IndyObjectHandler;

public class IndyInterpreterTest extends InterpreterTest {
  @Override
  protected DefaultMustacheFactory createMustacheFactory() {
    DefaultMustacheFactory mustacheFactory = new CodegenMustacheFactory(root);
    mustacheFactory.setObjectHandler(new IndyObjectHandler());
    return mustacheFactory;
  }
}
