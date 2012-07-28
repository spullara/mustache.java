package com.github.mustachejava.util;

import java.io.IOException;
import java.io.Writer;

/**
 * Manages a state machine that knows the context in an HTML document it is writing.
 */
public class StateAwareWriter<T extends State> extends Writer {

  // Delegate
  private Writer writer;
  private T state;

  public StateAwareWriter(Writer writer, T state) {
    this.writer = writer;
    this.state = state;
  }

  @Override
  public void write(char[] cbuf, int off, int len) throws IOException {
    state.nextState(cbuf, off, len);
    writer.write(cbuf, off, len);
  }

  @Override
  public void flush() throws IOException {
    writer.flush();
  }

  @Override
  public void close() throws IOException {
    flush();
    writer.close();
  }
}

