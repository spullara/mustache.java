package com.github.mustachejavabenchmarks.simple;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.reflect.SimpleObjectHandler;
import com.github.mustachejavabenchmarks.BenchmarkTest;

/**
 * Compare compilation with interpreter.
 * <p/>
 * User: sam
 * Date: 5/14/11
 * Time: 9:28 PM
 */
public class SimpleBenchmarkTest extends BenchmarkTest {
  @Override
  public void testCompiler() {
  }

  @Override
  protected DefaultMustacheFactory createMustacheFactory() {
    DefaultMustacheFactory mf = new DefaultMustacheFactory();
    mf.setObjectHandler(new SimpleObjectHandler());
    return mf;
  }
}
