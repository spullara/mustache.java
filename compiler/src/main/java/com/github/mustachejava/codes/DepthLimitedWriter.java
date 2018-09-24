package com.github.mustachejava.codes;

import com.github.mustachejava.util.AbstractIndentWriter;
import com.github.mustachejava.util.IndentWriter;

import java.io.FilterWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.atomic.AtomicInteger;

class DepthLimitedWriter extends AbstractIndentWriter {
  private AtomicInteger depth = new AtomicInteger(0);
  public DepthLimitedWriter(IndentWriter writer) {
    super(writer);
  }

  public int incr() {
    return depth.incrementAndGet();
  }

  public int decr() {
    return depth.decrementAndGet();
  }
}
