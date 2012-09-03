package com.github.mustachejavabench;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.codegen.CodegenMustacheFactory;
import com.github.mustachejavabenchmarks.JsonInterpreterTest;

/**
 * Tests for the compiler.
 * <p/>
 * User: sam
 * Date: May 3, 2010
 * Time: 10:23:54 AM
 */
public class CodegenJsonInterpreterTest extends JsonInterpreterTest {
  @Override
  protected DefaultMustacheFactory createMustacheFactory() {
    return new CodegenMustacheFactory(root);
  }
}
