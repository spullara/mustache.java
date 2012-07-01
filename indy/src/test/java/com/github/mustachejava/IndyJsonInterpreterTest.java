package com.github.mustachejava;

import com.github.mustachejava.indy.IndyObjectHandler;
import com.github.mustachejavabenchmarks.JsonInterpreterTest;

/**
 * Tests for the compiler.
 * <p/>
 * User: sam
 * Date: May 3, 2010
 * Time: 10:23:54 AM
 */
public class IndyJsonInterpreterTest extends JsonInterpreterTest {
  @Override
  protected DefaultMustacheFactory createMustacheFactory() {
    DefaultMustacheFactory mustacheFactory = super.createMustacheFactory();
    mustacheFactory.setObjectHandler(new IndyObjectHandler());
    return mustacheFactory;
  }
}
