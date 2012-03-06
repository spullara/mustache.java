package com.github.mustachejavabenchmarks;

import java.io.IOException;
import java.io.Writer;

/**
* Created with IntelliJ IDEA.
* User: spullara
* Date: 3/5/12
* Time: 7:04 PM
* To change this template use File | Settings | File Templates.
*/
public class NullWriter extends Writer {
  @Override
  public void write(int c) throws IOException {
  }

  @Override
  public void write(char[] cbuf) throws IOException {
  }

  @Override
  public void write(String str) throws IOException {
  }

  @Override
  public void write(String str, int off, int len) throws IOException {
  }

  @Override
  public Writer append(CharSequence csq) throws IOException {
    return this;
  }

  @Override
  public Writer append(CharSequence csq, int start, int end) throws IOException {
    return this;
  }

  @Override
  public Writer append(char c) throws IOException {
    return this;
  }

  @Override
  public void write(char[] cbuf, int off, int len) throws IOException {
  }

  @Override
  public void flush() throws IOException {
  }

  @Override
  public void close() throws IOException {
  }
}
