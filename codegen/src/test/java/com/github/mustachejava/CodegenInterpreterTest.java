package com.github.mustachejava;

import com.github.mustachejava.codegen.CodegenMustacheFactory;

import java.io.IOException;

public class CodegenInterpreterTest extends InterpreterTest {
  @Override
  protected DefaultMustacheFactory createMustacheFactory() {
    return new CodegenMustacheFactory(root);
  }

  @Override
  public void testIsNotEmpty() throws IOException {
    super.testIsNotEmpty();
  }
}
