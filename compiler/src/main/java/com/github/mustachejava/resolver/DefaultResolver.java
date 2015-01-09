package com.github.mustachejava.resolver;

import com.github.mustachejava.MustacheResolver;
import java.io.File;
import java.io.Reader;

/**
 * Mustache resolver that provides the default functionality
 * that the default mustache factory relies on by combining
 * the classpath and the filesystem resolver.
 */
public class DefaultResolver implements MustacheResolver {

  private final ClasspathResolver classpathResolver;
  private final FileSystemResolver fileSystemResolver;

  public DefaultResolver() {
    this.fileSystemResolver = new FileSystemResolver();
    this.classpathResolver = new ClasspathResolver();
  }

  /**
   * Use the classpath to resolve mustache templates.
   *
   * @param resourceRoot where in the classpath to find the templates
   */
  public DefaultResolver(String resourceRoot) {
    this.classpathResolver = new ClasspathResolver(resourceRoot);
    this.fileSystemResolver = new FileSystemResolver();
  }

  /**
   * Use the file system to resolve mustache templates.
   *
   * @param fileRoot where in the file system to find the templates
   */
  public DefaultResolver(File fileRoot) {
    this.fileSystemResolver = new FileSystemResolver(fileRoot);
    this.classpathResolver = new ClasspathResolver();
  }

  @Override
  public Reader getReader(String resourceName) {
      Reader reader = classpathResolver.getReader(resourceName);
      if(reader == null) {
        reader = fileSystemResolver.getReader(resourceName);
      }
      return reader;
  }

}
