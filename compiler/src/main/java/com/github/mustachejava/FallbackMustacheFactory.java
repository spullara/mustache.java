package com.github.mustachejava;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * Mustache.java factory with a fallback mechanism for locating resources.
 * <p/>
 * (Some parts are based on DefaultMustacheFactory code that is Copyright 2010 RightTime, Inc.)
 *
 * @author gw0 [http://gw.tnode.com/] <gw.2012@tnode.com>
 */
public class FallbackMustacheFactory extends DefaultMustacheFactory {

  /**
   * List of fallback resource roots to search through.
   */
  private Object[] resourceRoots;

  /**
   * Simple constructor for a fallback Mustache.java factory.
   *
   * @param resourceRoot normal resource root
   * @param fallbackRoot fallback alternative root
   */
  public FallbackMustacheFactory(String resourceRoot, String fallbackRoot) {
    this(new String[]{resourceRoot, fallbackRoot});
  }

  /**
   * Simple constructor for a fallback Mustache.java factory.
   *
   * @param resourceRoot normal resource root
   * @param fallbackRoot fallback alternative root
   */
  public FallbackMustacheFactory(File fileRoot, File fallbackRoot) {
    this(new File[]{fileRoot, fallbackRoot});
  }

  /**
   * Generic constructor for a fallback Mustache.java factory.
   *
   * @param resourceRoots array of fallback resource roots as String or File
   */
  public FallbackMustacheFactory(Object... resourceRoots) {
    super();

    for (Object resourceObj : resourceRoots) {
      if (resourceObj instanceof String) {  // for String
        String resourceRoot = (String) resourceObj;
        if (!resourceRoot.endsWith("/")) resourceRoot += "/";
        // TODO: this is a bug. we don't update the list.
      } else if (resourceObj instanceof File) {  // for File
        File fileRoot = (File) resourceObj;
        if (!fileRoot.exists()) {
          throw new MustacheException(fileRoot + " does not exist");
        }
        if (!fileRoot.isDirectory()) {
          throw new MustacheException(fileRoot + " is not a directory");
        }
      } else if (resourceObj == null) {
        // for null
      } else {
        throw new MustacheException("Invalid constructor parameter: " + resourceObj.toString());
      }
    }
    this.resourceRoots = resourceRoots;
  }

  /**
   * Return a reader for accessing resource files.
   *
   * @param resourceName resource name relative to one of the fallback resource roots
   * @return resource file reader
   */
  @Override
  public Reader getReader(String resourceName) {
    Exception lastException = null;

    for (Object resourceObj : this.resourceRoots) {
      try {
        InputStream is = null;
        if (resourceObj instanceof String) {  // class resource loader only for String
          String resourceRoot = (String) resourceObj;
          ClassLoader ccl = Thread.currentThread().getContextClassLoader();
          is = ccl.getResourceAsStream(resourceRoot + resourceName);
        }

        if (is == null) {
          File file;
          if (resourceObj instanceof String) {  // for String
            file = new File((String) resourceObj, resourceName);
          } else if (resourceObj instanceof File) {  // for File
            file = new File((File) resourceObj, resourceName);
          } else {  // for null
            file = new File(resourceName);
          }

          if (file.exists() && file.isFile()) {
            try {
              is = new FileInputStream(file);
            } catch (FileNotFoundException e) {
              throw new MustacheException("Found file, could not open: " + file, e);
            }
          }
        }

        if (is == null) {
          throw new MustacheNotFoundException(resourceName);
        } else {
          return new BufferedReader(new InputStreamReader(is, "UTF-8"));
        }

      } catch (Exception e) {
        lastException = e;
      }
    }
    throw new MustacheNotFoundException(resourceName, lastException);
  }

}
