package com.github.mustachejava.util;

import com.github.mustachejava.MustacheException;

import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.CountDownLatch;

/**
 * Buffer content while a future is being evaluated in another thread.
 */
public class LatchedWriter extends Writer {

  // Write to the buffer while latched, when unlatched, write the
  // buffer to the underlying writer and any future writes
  private final CountDownLatch latch = new CountDownLatch(1);

  // A buffer that holds writes until the latch is unlatched
  private final StringBuilder buffer = new StringBuilder();

  // The underlying writer
  private final Writer writer;

  // This is set when the latch holder fails
  private volatile Throwable e;

  public LatchedWriter(Writer writer) {
    this.writer = writer;
  }

  // Call this when your processing is complete
  public synchronized void done() throws IOException {
    writer.append(buffer);
    latch.countDown();
  }

  // If you fail to complete, put an exception here
  public void failed(Throwable e) {
    this.e = e;
    latch.countDown();
  }

  @Override
  public synchronized void write(char[] cbuf, int off, int len) throws IOException {
    checkException();
    if (latch.getCount() == 0) {
      writer.write(cbuf, off, len);
    } else {
      buffer.append(cbuf, off, len);
    }
  }

  private void checkException() throws IOException {
    if (e != null) {
      if (e instanceof IOException) {
        throw ((IOException) e);
      }
      throw new IOException(e);
    }
  }

  @Override
  public void flush() throws IOException {
    checkException();
    if (latch.getCount() == 0) {
      synchronized (this) {
        writer.flush();
      }
    }
  }

  @Override
  public void close() throws IOException {
    checkException();
    await();
    flush();
    writer.close();
  }

  public void await() {
    try {
      latch.await();
    } catch (InterruptedException e) {
      throw new MustacheException("Interrupted while waiting for completion", e);
    }
  }

}
