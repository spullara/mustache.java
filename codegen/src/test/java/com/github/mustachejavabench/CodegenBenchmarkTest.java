package com.github.mustachejavabench;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.codegen.CodegenMustacheFactory;
import com.github.mustachejavabenchmarks.BenchmarkTest;

/**
 * Compare compilation with interpreter.
 * <p/>
 * User: sam
 * Date: 5/14/11
 * Time: 9:28 PM
 */
public class CodegenBenchmarkTest extends BenchmarkTest {
  @Override
  public void testCompiler() {
  }

  @Override
  protected DefaultMustacheFactory createMustacheFactory() {
    return new CodegenMustacheFactory(root);
  }
}
