package com.github.mustachejava.resolver;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

import com.github.mustachejava.MustacheException;
import com.github.mustachejava.MustacheResolver;

/**
 * MustacheResolver implementation that resolves
 * mustache files from the filesystem.
 */
public class FileSystemResolver implements MustacheResolver {

  private final File fileRoot;
  private final Path pathRoot;

  public FileSystemResolver() {
    this.fileRoot = null;
    this.pathRoot = null;
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
    this.pathRoot = null;
  }

  /**
   * Use the file system to resolve mustache templates.
   *
   * @param pathRoot where in the file system to find the templates
   */
  public FileSystemResolver(Path pathRoot) {
    // This is not exactly the same as FileSystemResolver(pathRoot.toFile()).
    // pathRoot.toFile() throws UnsupportedOperationException when
    // pathRoot.getFileSystem() is not the default file system.
    //
    // The other constructors could be rewritten to delegate to this one, but
    // that would risk changing existing behavior more than this approach does.
    if (!Files.exists(pathRoot)) {
      throw new MustacheException(pathRoot + " does not exist");
    }
    if (!Files.isDirectory(pathRoot)) {
      throw new MustacheException(pathRoot + " is not a directory");
    }
    this.fileRoot = null;
    this.pathRoot = pathRoot;
  }

  @Override
  public Reader getReader(String resourceName) {
    if (pathRoot != null) {
      return getReaderFromPath(resourceName);
    }
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

  private Reader getReaderFromPath(String resourceName) {
    Path file;
    try {
      // Intentionally avoid using pathRoot.resolve(resourceName), which behaves
      // differently than our java.io.File code when resourceName is an
      // absolute path.
      file = pathRoot.getFileSystem().getPath(pathRoot.toString(), resourceName);
    } catch (InvalidPathException ignored) {
      // Mimic the java.io.File behavior for invalid resourceName paths.
      return null;
    }
    if (!Files.isRegularFile(file)) {
      return null;
    }
    try {
      Path canonicalFile = file.toRealPath();
      Path canonicalRoot = pathRoot.toRealPath();
      if (!canonicalFile.startsWith(canonicalRoot)) {
        throw new MustacheException("File not under root, file=" + canonicalFile + ", root=" + canonicalRoot);
      }
      return Files.newBufferedReader(file);
    } catch (IOException e) {
      throw new MustacheException("Found file, could not open: " + file, e);
    }
  }
}
