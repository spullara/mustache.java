package com.github.mustachejava.codes;

import java.io.FilterWriter;
import java.io.Writer;
import java.util.concurrent.atomic.AtomicInteger;

class DepthLimitedWriter extends FilterWriter {
  private AtomicInteger depth = new AtomicInteger(0);
  public DepthLimitedWriter(Writer writer) {
    super(writer);
  }

  public int incr() {
    return depth.incrementAndGet();
  }

  public int decr() {
    return depth.decrementAndGet();
  }
}
