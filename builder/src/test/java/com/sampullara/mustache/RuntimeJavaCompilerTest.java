package com.sampullara.mustache;

import com.sampullara.util.RuntimeJavaCompiler;
import junit.framework.TestCase;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

/**
 * Test that certain esoteric features work.
 * <p/>
 * User: sam
 * Date: May 17, 2010
 * Time: 7:21:51 PM
 */
public class RuntimeJavaCompilerTest extends TestCase {
  public void testClassLoader() throws IOException, ClassNotFoundException {
    ClassLoader loader = RuntimeJavaCompiler.compile(new PrintWriter(System.out, true), "Test", "public class Test {}");
    Class clazz = loader.loadClass("Test");
    InputStream stream = loader.getResourceAsStream("/Test.class");
    assertTrue(stream != null);
  }
}
