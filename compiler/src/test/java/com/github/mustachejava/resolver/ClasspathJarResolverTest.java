package com.github.mustachejava.resolver;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.Reader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ClasspathJarResolverTest {
  @Rule
  public TemporaryFolder temporary = new TemporaryFolder();

  @Test
  public void readsFileWhenClassLoaderAlsoResolvesTrailingSlash() throws Exception {
    assertResource("templates/view.mustache", true, "hello");
  }

  @Test
  public void readsFileWithOrdinaryJarClassLoader() throws Exception {
    assertResource("templates/view.mustache", false, "hello");
  }

  @Test
  public void rejectsDirectoryWithoutTrailingSlash() throws Exception {
    assertResource("templates", false, null);
    assertResource("templates", true, null);
  }

  @Test
  public void rejectsDirectoryWithTrailingSlash() throws Exception {
    assertResource("templates/", false, null);
    assertResource("templates/", true, null);
  }

  @Test
  public void missingJarResourceReturnsNull() throws Exception {
    assertResource("missing.mustache", false, null);
    assertResource("missing.mustache", true, null);
  }

  private void assertResource(String resource, boolean normalizeTrailingSlash, String expected)
      throws Exception {
    File jar = temporary.newFile();
    try (JarOutputStream output = new JarOutputStream(new FileOutputStream(jar))) {
      output.putNextEntry(new JarEntry("templates/"));
      output.closeEntry();
      output.putNextEntry(new JarEntry("templates/view.mustache"));
      output.write("hello".getBytes(StandardCharsets.UTF_8));
      output.closeEntry();
    }
    ClassLoader original = Thread.currentThread().getContextClassLoader();
    try (URLClassLoader loader = new URLClassLoader(new URL[] {jar.toURI().toURL()}, null) {
      @Override
      public URL getResource(String name) {
        if (normalizeTrailingSlash && name.endsWith("/")) {
          name = name.substring(0, name.length() - 1);
        }
        return super.getResource(name);
      }
    }) {
      Thread.currentThread().setContextClassLoader(loader);
      try (Reader reader = new ClasspathResolver().getReader(resource)) {
        if (expected == null) {
          assertNull(reader);
        } else {
          assertNotNull(reader);
          StringBuilder text = new StringBuilder();
          for (int c; (c = reader.read()) != -1;) {
            text.append((char) c);
          }
          assertEquals(expected, text.toString());
        }
      }
    } finally {
      Thread.currentThread().setContextClassLoader(original);
    }
  }
}
