package com.github.mustachejava.codes;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import org.junit.Test;

public class CodeCompileTest {
  @Test
  public void testCompile() {
    DefaultMustacheFactory dmf = new DefaultMustacheFactory();
    Mustache mustache = dmf.compile("compiletest.mustache");
  }
}
