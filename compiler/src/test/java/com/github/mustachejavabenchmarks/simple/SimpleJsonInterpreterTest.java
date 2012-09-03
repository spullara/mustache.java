package com.github.mustachejavabenchmarks.simple;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.reflect.SimpleObjectHandler;
import com.github.mustachejavabenchmarks.JsonInterpreterTest;

/**
 * TODO: Edit this
 * <p/>
 * User: sam
 * Date: 7/7/12
 * Time: 4:30 PM
 */
public class SimpleJsonInterpreterTest extends JsonInterpreterTest {
  @Override
  protected DefaultMustacheFactory createMustacheFactory() {
    DefaultMustacheFactory mf = super.createMustacheFactory();
    mf.setObjectHandler(new SimpleObjectHandler());
    return mf;
  }
}
