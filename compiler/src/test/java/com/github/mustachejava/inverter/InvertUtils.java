package com.github.mustachejava.inverter;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;

/**
 * TODO: Edit this
 * <p/>
 * User: sam
 * Date: 9/7/13
 * Time: 10:14 PM
 */
public class InvertUtils {
  public Path getPath(String name) {
    FileSystem fs = FileSystems.getDefault();
    Path path = fs.getPath(name);
    if (path.toFile().exists()) {
      return path;
    } else {
      return fs.getPath("compiler/" + name);
    }
  }
}
