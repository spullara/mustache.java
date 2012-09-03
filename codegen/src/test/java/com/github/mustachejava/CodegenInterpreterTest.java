package com.github.mustachejava;

import com.github.mustachejava.codegen.CodegenMustacheFactory;

public class CodegenInterpreterTest extends InterpreterTest {
  @Override
  protected DefaultMustacheFactory createMustacheFactory() {
    return new CodegenMustacheFactory(root);
  }
}
