package com.github.mustachejava;

import com.github.mustachejava.indy.IndyObjectHandler;

/**
 * TODO: Edit this
 * <p/>
 * User: sam
 * Date: 7/1/12
 * Time: 12:20 AM
 */
public class IndyInterpreterTest extends InterpreterTest {
  @Override
  protected DefaultMustacheFactory createMustacheFactory() {
    DefaultMustacheFactory mustacheFactory = super.createMustacheFactory();
    mustacheFactory.setObjectHandler(new IndyObjectHandler());
    return mustacheFactory;
  }
}
