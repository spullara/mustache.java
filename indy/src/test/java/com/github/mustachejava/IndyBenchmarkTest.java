package com.github.mustachejava;

import com.github.mustachejava.codegen.CodegenMustacheFactory;
import com.github.mustachejava.indy.IndyObjectHandler;
import com.github.mustachejavabenchmarks.BenchmarkTest;

/**
 * Compare compilation with interpreter.
 * <p/>
 * User: sam
 * Date: 5/14/11
 * Time: 9:28 PM
 */
public class IndyBenchmarkTest extends BenchmarkTest {
  @Override
  public void testCompiler() {
  }

  @Override
  protected DefaultMustacheFactory createMustacheFactory() {
    DefaultMustacheFactory mustacheFactory = new CodegenMustacheFactory(root);
    mustacheFactory.setObjectHandler(new IndyObjectHandler());
    return mustacheFactory;
  }
}
