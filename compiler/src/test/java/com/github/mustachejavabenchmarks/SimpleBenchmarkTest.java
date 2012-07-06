package com.github.mustachejavabenchmarks;

import com.github.mustachejava.*;
import com.github.mustachejava.reflect.SimpleObjectHandler;
import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.Executors;

/**
 * Compare compilation with interpreter.
 * <p/>
 * User: sam
 * Date: 5/14/11
 * Time: 9:28 PM
 */
public class SimpleBenchmarkTest extends BenchmarkTest {
  @Override
  protected DefaultMustacheFactory createMustacheFactory() {
    DefaultMustacheFactory mf = new DefaultMustacheFactory();
    mf.setObjectHandler(new SimpleObjectHandler());
    return mf;
  }
}
