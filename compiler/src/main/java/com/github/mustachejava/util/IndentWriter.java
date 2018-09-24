package com.github.mustachejava.util;

import java.io.IOException;
import java.io.Writer;

public abstract class IndentWriter extends Writer {
  public abstract void writeLines(char[][] lines) throws IOException;
  public abstract void flushIndent() throws IOException;
  public abstract void setPrependIndent();
}
