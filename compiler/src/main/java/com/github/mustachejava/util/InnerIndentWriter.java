package com.github.mustachejava.util;

import java.io.IOException;

public class InnerIndentWriter extends AbstractIndentWriter {

  private final char[] indent;
  private boolean prependIndent = false;

  public InnerIndentWriter(IndentWriter inner, char[] indent) {
    super(inner);
    this.indent = indent;
  }

  @Override
  public void write(char[] chars, int off, int len) throws IOException {
    if (len <= 0) {
      return;
    }

    this.flushIndent();
    super.write(chars, off, len);
  }

  @Override
  public void writeLines(char[][] lines) throws IOException {
    for (int i = 0; i < lines.length - 1; ++i) {
      write(lines[i], 0, lines[i].length);
      write('\n');
      this.prependIndent = true;
    }
    write(lines[lines.length - 1], 0, lines[lines.length - 1].length);
  }

  @Override
  public void flushIndent() throws IOException {
    super.setPrependIndent();
    if (this.prependIndent) {
      this.prependIndent = false;
      write(indent);
    }
  }

  @Override
  public void setPrependIndent() {
    super.setPrependIndent();
    this.prependIndent = true;
  }
}
