package com.github.mustachejava.util;

import java.io.IOException;
import java.io.Writer;

public class IndentWriter extends Writer {

  public final Writer inner;
  private final String indent;
  private boolean prependIndent = false;

  public IndentWriter(Writer inner, String indent) {
    this.inner = inner;
    this.indent = indent;
  }

  @Override
  public void write(char[] chars, int off, int len) throws IOException {
    int newOff = off;
    for (int i = newOff; i < len; ++i) {
      if (chars[i] == '\n') {
        // write character up to newline
        writeLine(chars, newOff, i + 1 - newOff);
        this.prependIndent = true;

        newOff = i + 1;
      }
    }
    writeLine(chars, newOff, len - (newOff - off));
  }

  public void flushIndent() throws IOException {
    if (this.prependIndent) {
      inner.append(indent);
      this.prependIndent = false;
    }
  }

  private void writeLine(char[] chars, int off, int len) throws IOException {
    if (len <= 0) {
      return;
    }

    this.flushIndent();
    inner.write(chars, off, len);
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
