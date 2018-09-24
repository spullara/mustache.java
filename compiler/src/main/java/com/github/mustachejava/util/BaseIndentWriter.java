package com.github.mustachejava.util;

import java.io.IOException;
import java.io.Writer;

public class BaseIndentWriter extends IndentWriter {

  public final Writer inner;

  public BaseIndentWriter(Writer inner) {
    this.inner = inner;
  }

  @Override
  public void writeLines(char[][] lines) throws IOException {
    for (int i = 0; i < lines.length - 1; ++i) {
      write(lines[i], 0, lines[i].length);
      write('\n');
    }
    write(lines[lines.length - 1], 0, lines[lines.length - 1].length);
  }

  @Override
  public void flushIndent() {
  }

  @Override
  public void setPrependIndent() {
  }

  @Override
  public void write(char[] chars, int i, int i1) throws IOException {
    inner.write(chars, i, i1);
  }

  @Override
  public void flush() throws IOException {
    inner.flush();
  }

  @Override
  public void close() throws IOException {
    inner.close();
  }
}
