package com.github.mustachejava.resolver;

import com.github.mustachejava.MustacheException;
import com.github.mustachejava.MustacheResolver;
import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * MustacheResolver implementation that resolves
 * mustache files from the filesystem.
 */
public class FileSystemResolver implements MustacheResolver {

  private final File fileRoot;

  public FileSystemResolver() {
    this.fileRoot = null;
  }

  /**
   * Use the file system to resolve mustache templates.
   *
   * @param fileRoot where in the file system to find the templates
   */
  public FileSystemResolver(File fileRoot) {
    if (!fileRoot.exists()) {
      throw new MustacheException(fileRoot + " does not exist");
    }
    if (!fileRoot.isDirectory()) {
      throw new MustacheException(fileRoot + " is not a directory");
    }
    this.fileRoot = fileRoot;
  }

  @Override
  public Reader getReader(String resourceName) {
    InputStream is = null;
    File file = fileRoot == null ? new File(resourceName) : new File(fileRoot, resourceName);
    if (file.exists() && file.isFile()) {
      try {
        // Check to make sure that the file is under the file root or current directory.
        // Without this check you might accidentally open a security whole when exposing
        // mustache templates to end users.
        File checkRoot = fileRoot == null ? new File("").getCanonicalFile() : fileRoot.getCanonicalFile();
        File parent = file.getCanonicalFile();
        while ((parent = parent.getParentFile()) != null) {
          if (parent.equals(checkRoot)) break;
        }
        if (parent == null) {
          throw new MustacheException("File not under root: " + checkRoot.getAbsolutePath());
        }
        is = new FileInputStream(file);
      } catch (IOException e) {
        throw new MustacheException("Found file, could not open: " + file, e);
      }
    }
    if (is != null) {
      return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
    } else {
      return null;
    }
  }
}
