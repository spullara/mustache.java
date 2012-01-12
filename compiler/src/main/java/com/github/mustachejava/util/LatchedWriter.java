package com.github.mustachejava.util;

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

  // true when we have written the buffer to the underlying writer
  private boolean isWritten;

  // true when we have flushed and closed the underlying writer
  private boolean isClosed;

  // This is set when the latch holder fails
  private volatile Throwable e;

  public LatchedWriter(Writer writer) {
    this.writer = writer;
  }

  // Call this when your processing is complete
  public synchronized void done() {
    latch.countDown();
  }

  // If you fail to complete, put an exception here
  public void failed(Throwable e) {
    this.e = e;
    done();
  }

  @Override
  public synchronized void write(char[] cbuf, int off, int len) throws IOException {
    checkException();
    if (latch.getCount() == 0) {
      checkWrite();
      writer.write(cbuf, off, len);
    } else {
      buffer.append(cbuf, off, len);
    }
  }

  private void checkWrite() throws IOException {
    if (!isWritten) {
      isWritten = true;
      writer.append(buffer);
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
      checkException();
      synchronized (this) {
        checkWrite();
        writer.flush();
      }
    }
  }

  @Override
  public void close() throws IOException {
    checkException();
    synchronized (this) {
      if (isClosed) throw new IOException("Alread closed");
      else isClosed = true;
    }
    try {
      latch.await();
    } catch (InterruptedException e1) {
      throw new IOException("Interrupted while waiting for completion", e1);
    }
    flush();
    writer.close();
  }

}
